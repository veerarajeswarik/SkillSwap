// ============================================================
// SkillSwap frontend - plain JavaScript, talks to the Spring Boot REST API
// ============================================================

const API_BASE = "http://localhost:8080/api";
const MEMBER_KEY = "skillswap.currentMemberId";

// Everything the page currently knows, kept in one place.
const state = {
    currentMember: null,   // the member we are "acting as" (full object from the API)
    members: [],           // all members, for the "Acting as" dropdown
    activeSkills: []       // skills from GET /api/skills
};

let currentPage = "home";
let preselectedSkillId = null; // set when "Request" is clicked on a skill card


// ============================================================
// 1. API COMMUNICATION
// ============================================================

// An Error that also remembers the HTTP status and per-field validation errors
// sent by GlobalExceptionHandler ( { message, errors: { field: text } } ).
class ApiError extends Error {
    constructor(message, status, fieldErrors) {
        super(message);
        this.status = status;
        this.fieldErrors = fieldErrors || {};
    }
}

// The single function that every API call goes through.
//   apiRequest("/members")                                   -> GET
//   apiRequest("/skills", { method: "POST", body: {...} })   -> POST with JSON
async function apiRequest(path, options = {}) {
    const config = {
        method: options.method || "GET",
        headers: { "Accept": "application/json" }
    };

    if (options.body !== undefined) {
        // Tell Spring the body is JSON, so @RequestBody can read it.
        config.headers["Content-Type"] = "application/json";
        // Convert the JavaScript object into a JSON text string.
        config.body = JSON.stringify(options.body);
    }

    let response;
    try {
        response = await fetch(API_BASE + path, config);
    } catch (networkError) {
        // fetch() only throws when there is NO response at all (server down, CORS blocked...).
        throw new ApiError("Cannot reach the server. Is the backend running on http://localhost:8080?", 0);
    }

    // Read the body as text first: some responses could be empty.
    const text = await response.text();
    let data = null;
    if (text) {
        try {
            data = JSON.parse(text);
        } catch (parseError) {
            data = null;
        }
    }

    // fetch() does NOT throw for 400/404/409/500. We must check response.ok ourselves.
    if (!response.ok) {
        const message = (data && data.message) || `Request failed with status ${response.status}`;
        throw new ApiError(message, response.status, data && data.errors);
    }

    return data;
}


// ============================================================
// 2. SMALL HELPERS
// ============================================================

function $(selector) {
    return document.querySelector(selector);
}

// Prevents HTML injection (XSS): a skill named "<script>..." is shown as text, not run.
function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}

function formatNumber(value) {
    return Number(value).toLocaleString(undefined, { maximumFractionDigits: 2 });
}

function formatDate(isoText) {
    return isoText ? new Date(isoText).toLocaleString() : "-";
}

// "" -> null (so the server's @NotNull message appears), "2" -> 2
function toNumberOrNull(value) {
    return value === "" || value === null || value === undefined ? null : Number(value);
}

// All the form's inputs as a plain object: { name: "...", email: "..." }
function formValues(form) {
    return Object.fromEntries(new FormData(form));
}

function readStorage(key) {
    try {
        return localStorage.getItem(key);
    } catch (e) {
        return null;
    }
}

function writeStorage(key, value) {
    try {
        if (value === null) {
            localStorage.removeItem(key);
        } else {
            localStorage.setItem(key, value);
        }
    } catch (e) {
        // Storage can be blocked (private mode, file:// in some browsers); the app still works.
    }
}

let toastTimer = null;

function toast(message, type = "success") {
    const box = $("#toast");
    box.textContent = message;
    box.className = `toast ${type}`;
    box.hidden = false;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => { box.hidden = true; }, 5000);
}

// Shows an ApiError: field messages under inputs (if a form is given) + a toast.
function showError(error, form) {
    const fieldMessages = Object.values(error.fieldErrors || {});

    if (form) {
        for (const [field, message] of Object.entries(error.fieldErrors || {})) {
            const slot = form.querySelector(`[data-error-for="${field}"]`);
            if (slot) {
                slot.textContent = message;
            }
            if (form.elements[field]) {
                form.elements[field].classList.add("invalid");
            }
        }
    }

    toast(fieldMessages.length ? fieldMessages.join(" ") : error.message, "error");
}

function clearFieldErrors(form) {
    form.querySelectorAll(".field-error").forEach(slot => { slot.textContent = ""; });
    form.querySelectorAll(".invalid").forEach(input => input.classList.remove("invalid"));
}


// ============================================================
// 3. NAVIGATION (one HTML page, sections shown/hidden)
// ============================================================

const PAGE_LOADERS = {
    dashboard: loadDashboard,
    browse: loadBrowse,
    request: loadRequestForm,
    sessions: loadSessions,
    credits: loadCredits
};

async function showPage(name) {
    currentPage = name;
    const page = document.getElementById(name);

    document.querySelectorAll(".page").forEach(section => { section.hidden = true; });
    document.querySelectorAll(".nav button").forEach(button => {
        button.classList.toggle("active", button.dataset.go === name);
    });

    // Pages marked data-needs-member cannot work without a current member.
    if (page.hasAttribute("data-needs-member") && !state.currentMember) {
        $("#needMember").hidden = false;
        return;
    }

    page.hidden = false;

    const loader = PAGE_LOADERS[name];
    if (loader) {
        try {
            await loader();
        } catch (error) {
            showError(error);
        }
    }
}


// ============================================================
// 4. CURRENT MEMBER ("Acting as")
// ============================================================

async function loadMembers() {
    state.members = await apiRequest("/members");

    const options = state.members.map(member =>
        `<option value="${member.id}" title="${escapeHtml(member.email)}">${escapeHtml(member.name)} (#${member.id})</option>`
    );
    $("#memberSelect").innerHTML = `<option value="">-- choose member --</option>` + options.join("");

    if (state.currentMember) {
        $("#memberSelect").value = state.currentMember.id;
    }
}

async function setCurrentMember(id) {
    if (!id) {
        state.currentMember = null;
        writeStorage(MEMBER_KEY, null);
    } else {
        state.currentMember = await apiRequest(`/members/${id}`);
        writeStorage(MEMBER_KEY, String(id));
        $("#memberSelect").value = id;
    }
    updateHeader();
}

// Re-reads the member so the balance is always the server's latest value.
async function refreshCurrentMember() {
    if (state.currentMember) {
        state.currentMember = await apiRequest(`/members/${state.currentMember.id}`);
        updateHeader();
    }
}

function updateHeader() {
    const pill = $("#headerBalance");
    if (state.currentMember) {
        pill.textContent = `${formatNumber(state.currentMember.creditBalance)} credits`;
        pill.hidden = false;
    } else {
        pill.hidden = true;
    }
}


// ============================================================
// 5. REGISTER
// ============================================================

async function handleRegister(event) {
    event.preventDefault(); // stop the browser from reloading the page
    const form = event.target;
    clearFieldErrors(form);

    const values = formValues(form);
    const body = {
        name: values.name.trim(),
        email: values.email.trim(),
        password: values.password
    };

    try {
        const member = await apiRequest("/members/register", { method: "POST", body });
        form.reset();
        toast(`Welcome, ${member.name}! You start with ${formatNumber(member.creditBalance)} credits.`);
        await loadMembers();
        await setCurrentMember(member.id);
        showPage("dashboard");
    } catch (error) {
        showError(error, form);
    }
}


// ============================================================
// 6. DASHBOARD
// ============================================================

async function loadDashboard() {
    await refreshCurrentMember();
    const me = state.currentMember;

    // Three independent requests, sent at the same time.
    const [pending, requested, mySkills] = await Promise.all([
        apiRequest(`/sessions/provider/${me.id}?status=PENDING`),
        apiRequest(`/sessions/requester/${me.id}`),
        apiRequest(`/skills/provider/${me.id}`)
    ]);

    $("#dashName").textContent = me.name;
    $("#dashBalance").textContent = formatNumber(me.creditBalance);
    $("#dashPending").textContent = pending.length;
    $("#dashRequested").textContent = requested.length;
    $("#dashSkillCount").textContent = mySkills.length;

    $("#dashSkills").innerHTML = mySkills.length
        ? mySkills.map(skill => skillCardHtml(skill)).join("")
        : `<div class="card empty">You haven't offered any skills yet.
               <button class="btn small" data-go="offer">Offer one</button></div>`;
}


// ============================================================
// 7. BROWSE SKILLS
// ============================================================

function skillCardHtml(skill) {
    const me = state.currentMember;
    let button;

    if (!me) {
        button = `<button class="btn small" disabled title="Choose a member first">Request</button>`;
    } else if (skill.provider.id === me.id) {
        button = `<span class="hint">Your skill</span>`;
    } else if (skill.availableHours <= 0) {
        button = `<button class="btn small" disabled>Fully booked</button>`;
    } else {
        button = `<button class="btn small primary" data-request-skill="${skill.id}">Request</button>`;
    }

    return `
        <div class="card skill-card">
            <h3>${escapeHtml(skill.skillName)}</h3>
            <span class="teacher">taught by ${escapeHtml(skill.provider.name)}</span>
            <p class="desc">${skill.description ? escapeHtml(skill.description) : "<em>No description</em>"}</p>
            <div class="meta">
                <span class="badge ACTIVE">${formatNumber(skill.availableHours)} h available</span>
                ${button}
            </div>
        </div>`;
}

async function loadBrowse() {
    state.activeSkills = await apiRequest("/skills");
    renderSkillGrid();
}

function renderSkillGrid() {
    const query = $("#skillSearch").value.trim().toLowerCase();
    const matches = state.activeSkills.filter(skill =>
        skill.skillName.toLowerCase().includes(query) ||
        (skill.description || "").toLowerCase().includes(query) ||
        skill.provider.name.toLowerCase().includes(query)
    );

    $("#skillGrid").innerHTML = matches.length
        ? matches.map(skillCardHtml).join("")
        : `<div class="card empty">No skills found.</div>`;
}


// ============================================================
// 8. OFFER A SKILL
// ============================================================

async function handleOffer(event) {
    event.preventDefault();
    const form = event.target;
    clearFieldErrors(form);

    const values = formValues(form);
    const body = {
        providerId: state.currentMember.id,
        skillName: values.skillName.trim(),
        description: values.description.trim(),
        availableHours: toNumberOrNull(values.availableHours)
    };

    try {
        const skill = await apiRequest("/skills", { method: "POST", body });
        form.reset();
        toast(`"${skill.skillName}" is now listed with ${formatNumber(skill.availableHours)} hours.`);
        showPage("dashboard");
    } catch (error) {
        showError(error, form);
    }
}


// ============================================================
// 9. REQUEST A SESSION
// ============================================================

async function loadRequestForm() {
    await refreshCurrentMember();
    state.activeSkills = await apiRequest("/skills");

    const me = state.currentMember;
    const requestable = state.activeSkills.filter(skill =>
        skill.provider.id !== me.id && skill.availableHours > 0
    );

    const select = $("#requestSkill");
    select.innerHTML = requestable.length
        ? `<option value="">-- choose a skill --</option>` + requestable.map(skill =>
            `<option value="${skill.id}">${escapeHtml(skill.skillName)} - ${escapeHtml(skill.provider.name)}
             (${formatNumber(skill.availableHours)} h available)</option>`).join("")
        : `<option value="">No skills available to request</option>`;

    if (preselectedSkillId) {
        select.value = preselectedSkillId;
        preselectedSkillId = null;
    }
    updateRequestHint();
}

function updateRequestHint() {
    const me = state.currentMember;
    const skillId = Number($("#requestSkill").value);
    const skill = state.activeSkills.find(s => s.id === skillId);

    let hint = `You have ${formatNumber(me.creditBalance)} credits.`;
    if (skill) {
        hint += ` ${skill.provider.name} has ${formatNumber(skill.availableHours)} hours available.`;
    }
    hint += " Credits are only deducted after the teacher confirms the session.";
    $("#requestHint").textContent = hint;
}

async function handleRequest(event) {
    event.preventDefault();
    const form = event.target;
    clearFieldErrors(form);

    const values = formValues(form);
    const body = {
        requesterId: state.currentMember.id,
        skillOfferId: toNumberOrNull(values.skillOfferId),
        requestedHours: toNumberOrNull(values.requestedHours),
        message: values.message.trim()
    };

    try {
        const session = await apiRequest("/sessions", { method: "POST", body });
        form.reset();
        toast(`Request #${session.id} sent to ${session.provider.name}. It is PENDING until they confirm.`);
        showPage("sessions");
    } catch (error) {
        showError(error, form);
    }
}


// ============================================================
// 10. MY SESSIONS (confirm / reject)
// ============================================================

function statusBadge(status) {
    return `<span class="badge ${status}">${status}</span>`;
}

function emptyRow(columns, text) {
    return `<tr><td colspan="${columns}" class="empty">${text}</td></tr>`;
}

async function loadSessions() {
    const me = state.currentMember;
    const [asProvider, asRequester] = await Promise.all([
        apiRequest(`/sessions/provider/${me.id}`),
        apiRequest(`/sessions/requester/${me.id}`)
    ]);

    // Newest first
    asProvider.sort((a, b) => b.id - a.id);
    asRequester.sort((a, b) => b.id - a.id);

    $("#providerSessions").innerHTML = asProvider.length
        ? asProvider.map(session => {
            let action = "-";
            if (session.status === "PENDING") {
                action = `
                    <div class="actions">
                        <input id="hours-${session.id}" class="hours-input" type="number"
                               min="0.5" step="0.5" max="${session.requestedHours}"
                               value="${session.requestedHours}" title="Hours actually delivered">
                        <button class="btn small primary" data-confirm="${session.id}">Confirm</button>
                        <button class="btn small danger" data-reject="${session.id}">Reject</button>
                    </div>`;
            } else if (session.status === "CONFIRMED") {
                action = `${formatNumber(session.actualHoursDelivered)} h delivered`;
            }
            return `
                <tr>
                    <td>${session.id}</td>
                    <td>${escapeHtml(session.requester.name)}</td>
                    <td>${escapeHtml(session.skillOffer.skillName)}</td>
                    <td>${formatNumber(session.requestedHours)} h</td>
                    <td>${escapeHtml(session.message || "")}</td>
                    <td>${statusBadge(session.status)}</td>
                    <td>${action}</td>
                </tr>`;
        }).join("")
        : emptyRow(7, "Nobody has requested your skills yet.");

    $("#requesterSessions").innerHTML = asRequester.length
        ? asRequester.map(session => `
            <tr>
                <td>${session.id}</td>
                <td>${escapeHtml(session.provider.name)}</td>
                <td>${escapeHtml(session.skillOffer.skillName)}</td>
                <td>${formatNumber(session.requestedHours)} h</td>
                <td>${session.actualHoursDelivered != null ? formatNumber(session.actualHoursDelivered) + " h" : "-"}</td>
                <td>${statusBadge(session.status)}</td>
                <td>${formatDate(session.createdAt)}</td>
            </tr>`).join("")
        : emptyRow(7, "You haven't requested any sessions yet.");
}

async function confirmSession(sessionId) {
    const hours = toNumberOrNull(document.getElementById(`hours-${sessionId}`).value);

    try {
        const session = await apiRequest(`/sessions/${sessionId}/confirm`, {
            method: "PUT",
            body: { actualHoursDelivered: hours }
        });
        toast(`Session #${session.id} confirmed: ${formatNumber(hours)} credits moved from `
            + `${session.requester.name} to ${session.provider.name}.`);
        await refreshCurrentMember();
        await loadSessions();
    } catch (error) {
        showError(error);
    }
}

async function rejectSession(sessionId) {
    if (!window.confirm(`Reject session #${sessionId}? No credits will move.`)) {
        return;
    }
    try {
        await apiRequest(`/sessions/${sessionId}/reject`, { method: "PUT" });
        toast(`Session #${sessionId} rejected.`);
        await loadSessions();
    } catch (error) {
        showError(error);
    }
}


// ============================================================
// 11. CREDITS (balance + transaction history)
// ============================================================

async function loadCredits() {
    await refreshCurrentMember();
    const me = state.currentMember;
    $("#creditBalance").textContent = formatNumber(me.creditBalance);

    const entries = await apiRequest(`/credits/member/${me.id}`);

    $("#ledgerRows").innerHTML = entries.length
        ? entries.map(entry => {
            const sign = entry.transactionType === "CREDIT" ? "+" : "-";
            return `
                <tr>
                    <td>${formatDate(entry.createdAt)}</td>
                    <td>${statusBadge(entry.transactionType)}</td>
                    <td>${sign}${formatNumber(entry.hours)}</td>
                    <td>${escapeHtml(entry.description)}</td>
                    <td>#${entry.sessionRequest.id}</td>
                </tr>`;
        }).join("")
        : emptyRow(5, "No transactions yet. Credits move when a session is confirmed.");
}


// ============================================================
// 12. START-UP: connect buttons and forms to the functions above
// ============================================================

function wireEvents() {
    // One click listener for the whole page ("event delegation").
    // It also works for buttons created later with innerHTML.
    document.addEventListener("click", event => {
        const goTo = event.target.closest("[data-go]");
        if (goTo) {
            showPage(goTo.dataset.go);
            return;
        }

        const requestButton = event.target.closest("[data-request-skill]");
        if (requestButton) {
            preselectedSkillId = requestButton.dataset.requestSkill;
            showPage("request");
            return;
        }

        const confirmButton = event.target.closest("[data-confirm]");
        if (confirmButton) {
            confirmSession(confirmButton.dataset.confirm);
            return;
        }

        const rejectButton = event.target.closest("[data-reject]");
        if (rejectButton) {
            rejectSession(rejectButton.dataset.reject);
        }
    });

    $("#memberSelect").addEventListener("change", async event => {
        try {
            await setCurrentMember(event.target.value);
            showPage(currentPage); // reload the current page for the new member
        } catch (error) {
            showError(error);
        }
    });

    $("#registerForm").addEventListener("submit", handleRegister);
    $("#offerForm").addEventListener("submit", handleOffer);
    $("#requestForm").addEventListener("submit", handleRequest);
    $("#requestSkill").addEventListener("change", updateRequestHint);
    $("#skillSearch").addEventListener("input", renderSkillGrid);
}

async function init() {
    wireEvents();

    try {
        await loadMembers();
        const savedId = readStorage(MEMBER_KEY);
        if (savedId && state.members.some(member => String(member.id) === savedId)) {
            await setCurrentMember(savedId);
        }
    } catch (error) {
        showError(error);
    }

    showPage("home");
}

document.addEventListener("DOMContentLoaded", init);
