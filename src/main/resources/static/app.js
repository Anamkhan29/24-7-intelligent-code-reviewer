import {
  initializeApp
} from "https://www.gstatic.com/firebasejs/10.12.0/firebase-app.js";

import {
  getAuth,
  GoogleAuthProvider,
  signInWithPopup,
  signOut,
  onAuthStateChanged
} from "https://www.gstatic.com/firebasejs/10.12.0/firebase-auth.js";


/* =========================================================
   FIREBASE
   ========================================================= */

// For Firebase JS SDK v7.20.0 and later, measurementId is optional
const FIREBASE_CONFIG = {
  apiKey: "AIzaSyDrsE9ktWDB-AM-iLYZIHxOxgAnH7XZf1E",
  authDomain: "reviewer-b36ee.firebaseapp.com",
  projectId: "reviewer-b36ee",
  storageBucket: "reviewer-b36ee.firebasestorage.app",
  messagingSenderId: "787302907993",
  appId: "1:787302907993:web:6410ed2bea32e73fec7148",
};


const firebaseApp = initializeApp(FIREBASE_CONFIG);

const auth = getAuth(firebaseApp);

const googleProvider = new GoogleAuthProvider();


/* =========================================================
   DOM
   ========================================================= */

const loginButton =
    document.getElementById("loginButton");

const heroLoginButton =
    document.getElementById("heroLoginButton");

const logoutButton =
    document.getElementById("logoutButton");

const userInfo =
    document.getElementById("userInfo");

const signedOut =
    document.getElementById("signedOut");

const reviewConsole =
    document.getElementById("reviewConsole");

const codeInput =
    document.getElementById("codeInput");

const languageSelect =
    document.getElementById("language");

const reviewMode =
    document.getElementById("reviewMode");

const reviewButton =
    document.getElementById("reviewButton");

const loading =
    document.getElementById("loading");

const result =
    document.getElementById("result");

const growth =
    document.getElementById("growth");

const characterCount =
    document.getElementById("characterCount");

const languageHint =
    document.getElementById("languageHint");


/* =========================================================
   CONFIG
   ========================================================= */

const MAX_CODE_CHARS = 20000;


/* =========================================================
   LANGUAGE NAMES
   ========================================================= */

const LANGUAGE_NAMES = {
  auto: "Auto Detect",
  java: "Java",
  python: "Python",
  javascript: "JavaScript",
  typescript: "TypeScript",
  cpp: "C++",
  c: "C",
  csharp: "C#",
  go: "Go",
  rust: "Rust",
  kotlin: "Kotlin",
  swift: "Swift",
  php: "PHP",
  sql: "SQL",
  html: "HTML",
  css: "CSS"
};


/* =========================================================
   LANGUAGE DETECTION
   ========================================================= */

function detectLanguage(code) {

  if (!code || !code.trim()) {
    return "auto";
  }

  const text = code.trim();


  /* Java */

  if (
      /public\s+(class|interface|enum)\s+\w+/m.test(text) ||
      /System\.out\.println\s*\(/.test(text) ||
      /import\s+java\./.test(text) ||
      /public\s+static\s+void\s+main\s*\(/.test(text) ||
      /\bprivate\s+(static\s+)?(final\s+)?(String|int|long|double|boolean|List|Map)\b/.test(text)
  ) {
    return "java";
  }


  /* Python */

  if (
      /^\s*def\s+\w+\s*\(/m.test(text) ||
      /^\s*class\s+\w+.*:/m.test(text) ||
      /^\s*from\s+\w+(\.\w+)*\s+import\s+/m.test(text) ||
      /^\s*import\s+\w+/m.test(text) ||
      /if\s+__name__\s*==\s*["']__main__["']/.test(text) ||
      /print\s*\(/.test(text) && /:\s*$/m.test(text)
  ) {
    return "python";
  }


  /* TypeScript */

  if (
      /\binterface\s+\w+\s*\{/.test(text) ||
      /\btype\s+\w+\s*=/.test(text) ||
      /:\s*(string|number|boolean|any|unknown|void)\b/.test(text) ||
      /\bas\s+(string|number|boolean|any)\b/.test(text)
  ) {
    return "typescript";
  }


  /* JavaScript */

  if (
      /\b(const|let|var)\s+\w+\s*=/.test(text) ||
      /console\.log\s*\(/.test(text) ||
      /function\s+\w+\s*\(/.test(text) ||
      /=>\s*[{\w(]/.test(text) ||
      /require\s*\(/.test(text) ||
      /document\.querySelector\s*\(/.test(text)
  ) {
    return "javascript";
  }


  /* C++ */

  if (
      /#include\s*<iostream>/.test(text) ||
      /#include\s*<vector>/.test(text) ||
      /#include\s*<string>/.test(text) ||
      /std::cout/.test(text) ||
      /std::cin/.test(text) ||
      /std::vector/.test(text)
  ) {
    return "cpp";
  }


  /* C */

  if (
      /#include\s*<stdio\.h>/.test(text) ||
      /#include\s*<stdlib\.h>/.test(text) ||
      /printf\s*\(/.test(text) ||
      /scanf\s*\(/.test(text)
  ) {
    return "c";
  }


  /* C# */

  if (
      /using\s+System;/.test(text) ||
      /Console\.WriteLine\s*\(/.test(text) ||
      /\bnamespace\s+\w+/.test(text)
  ) {
    return "csharp";
  }


  /* Go */

  if (
      /package\s+main/.test(text) ||
      /func\s+main\s*\(/.test(text) ||
      /fmt\.Println\s*\(/.test(text) ||
      /:=/.test(text)
  ) {
    return "go";
  }


  /* Rust */

  if (
      /fn\s+main\s*\(\s*\)/.test(text) ||
      /\blet\s+mut\s+\w+/.test(text) ||
      /println!\s*\(/.test(text) ||
      /use\s+std::/.test(text)
  ) {
    return "rust";
  }


  /* Kotlin */

  if (
      /\bfun\s+main\s*\(/.test(text) ||
      /\bfun\s+\w+\s*\(/.test(text) ||
      /\bval\s+\w+\s*=/.test(text) ||
      /\bvar\s+\w+\s*=/.test(text)
  ) {
    return "kotlin";
  }


  /* Swift */

  if (
      /\bimport\s+Foundation\b/.test(text) ||
      /\bfunc\s+\w+\s*\(/.test(text) &&
      /\b(let|var)\s+\w+/.test(text)
  ) {
    return "swift";
  }


  /* PHP */

  if (
      /<\?php/.test(text) ||
      /\$\w+\s*=/.test(text) &&
      /\becho\s+/.test(text)
  ) {
    return "php";
  }


  /* SQL */

  if (
      /\bSELECT\b[\s\S]+\bFROM\b/i.test(text) ||
      /\bINSERT\s+INTO\b/i.test(text) ||
      /\bUPDATE\s+\w+\s+SET\b/i.test(text) ||
      /\bDELETE\s+FROM\b/i.test(text) ||
      /\bCREATE\s+TABLE\b/i.test(text)
  ) {
    return "sql";
  }


  /* HTML */

  if (
      /<!DOCTYPE\s+html>/i.test(text) ||
      /<html[\s>]/i.test(text) ||
      /<body[\s>]/i.test(text) ||
      /<div[\s>]/i.test(text)
  ) {
    return "html";
  }


  /* CSS */

  if (
      /[.#][\w-]+\s*\{[\s\S]*:[\s\S]*;[\s\S]*\}/.test(text)
  ) {
    return "css";
  }


  return "auto";
}


/* =========================================================
   UPDATE LANGUAGE UI
   ========================================================= */

function updateDetectedLanguage() {

  if (!languageSelect || !codeInput) {
    return;
  }

  const code = codeInput.value;

  const detected =
      detectLanguage(code);


  if (!code.trim()) {

    languageSelect.value = "auto";

    languageSelect.classList.remove(
        "language-detected"
    );

    languageHint.textContent =
        "Language will be detected automatically.";

    return;
  }


  if (detected === "auto") {

    languageSelect.classList.remove(
        "language-detected"
    );

    languageHint.textContent =
        "Language could not be confidently detected.";

    return;
  }


  const exists =
      Array.from(languageSelect.options)
          .some(option =>
              option.value === detected
          );


  if (!exists) {
    return;
  }


  languageSelect.value = detected;

  languageSelect.classList.add(
      "language-detected"
  );


  languageHint.textContent =
      `✓ Detected ${LANGUAGE_NAMES[detected]}`;
}


/* =========================================================
   CHARACTER COUNTER
   ========================================================= */

function updateCharacterCount() {

  if (!codeInput || !characterCount) {
    return;
  }

  const length =
      codeInput.value.length;


  characterCount.textContent =
      `${length.toLocaleString()} / ${MAX_CODE_CHARS.toLocaleString()}`;


  characterCount.classList.remove(
      "warning",
      "danger"
  );


  if (length > MAX_CODE_CHARS) {

    characterCount.classList.add(
        "danger"
    );

  } else if (length > MAX_CODE_CHARS * 0.85) {

    characterCount.classList.add(
        "warning"
    );
  }
}


/* =========================================================
   AUTHENTICATION
   ========================================================= */

async function login() {

  try {

    loginButton.disabled = true;

    heroLoginButton.disabled = true;

    await signInWithPopup(
        auth,
        googleProvider
    );

  } catch (error) {

    console.error(
        "Login failed:",
        error
    );

    alert(
        "Google sign-in failed. Please try again."
    );

  } finally {

    loginButton.disabled = false;

    heroLoginButton.disabled = false;
  }
}


async function logout() {

  try {

    await signOut(auth);

  } catch (error) {

    console.error(
        "Logout failed:",
        error
    );

    alert(
        "Unable to sign out. Please try again."
    );
  }
}


/* =========================================================
   AUTH STATE
   ========================================================= */

onAuthStateChanged(
    auth,
    async (user) => {

      console.log(
          "Auth state changed:",
          user?.email || "signed out"
      );


      if (user) {

        signedOut.classList.add(
            "hidden"
        );

        reviewConsole.classList.remove(
            "hidden"
        );

        loginButton.classList.add(
            "hidden"
        );

        logoutButton.classList.remove(
            "hidden"
        );

        userInfo.classList.remove(
            "hidden"
        );

        userInfo.textContent =
            user.email || "Signed in";


        await loadHistory();

      } else {

        signedOut.classList.remove(
            "hidden"
        );

        reviewConsole.classList.add(
            "hidden"
        );

        loginButton.classList.remove(
            "hidden"
        );

        logoutButton.classList.add(
            "hidden"
        );

        userInfo.classList.add(
            "hidden"
        );
      }
    }
);


/* =========================================================
   API AUTH TOKEN
   ========================================================= */

async function getAuthHeaders() {

  const user = auth.currentUser;

  if (!user) {
    throw new Error(
        "Please sign in first."
    );
  }

  const token =
      await user.getIdToken();


  return {
    "Authorization":
        `Bearer ${token}`,

    "Content-Type":
        "application/json"
  };
}


/* =========================================================
   REVIEW
   ========================================================= */

async function reviewCode() {

  const code =
      codeInput.value.trim();


  if (!code) {

    alert(
        "Please enter some code first."
    );

    codeInput.focus();

    return;
  }


  if (code.length > MAX_CODE_CHARS) {

    alert(
        `Code is too long. Maximum allowed length is ${MAX_CODE_CHARS.toLocaleString()} characters.`
    );

    return;
  }


  const language =
      languageSelect.value;


  const mode =
      reviewMode.value;


  loading.classList.remove(
      "hidden"
  );

  result.classList.add(
      "hidden"
  );

  reviewButton.disabled = true;


  try {

    const headers =
        await getAuthHeaders();


    const response =
        await fetch(
            "/api/review",
            {
              method: "POST",
              headers,
              body: JSON.stringify({
                code,
                language,
                review_mode: mode
              })
            }
        );


    const data =
        await response.json();


    if (!response.ok) {

      throw new Error(
          data.message ||
          data.error ||
          "Review request failed."
      );
    }


    renderReview(data);


    await loadHistory();


    result.scrollIntoView({
      behavior: "smooth",
      block: "start"
    });


  } catch (error) {

    console.error(
        "Review failed:",
        error
    );

    alert(
        error.message ||
        "Unable to review the code."
    );

  } finally {

    loading.classList.add(
        "hidden"
    );

    reviewButton.disabled = false;
  }
}


/* =========================================================
   RENDER REVIEW
   ========================================================= */

function renderReview(data) {

  result.classList.remove(
      "hidden"
  );


  /* Rating */

  const rating =
      Number(data.rating || 0);


  document.getElementById(
      "rating"
  ).textContent =
      rating.toFixed(1);


  document.getElementById(
      "scoreLabel"
  ).textContent =
      getScoreLabel(rating);


  animateScoreRing(rating);


  /* Summary */

  document.getElementById(
      "summary"
  ).textContent =
      data.summary ||
      "No summary was returned.";


  /* Language */

  const language =
      data.language ||
      languageSelect.value ||
      "auto";


  document.getElementById(
      "resultLanguage"
  ).textContent =
      LANGUAGE_NAMES[language] ||
      language;


  /* Findings */

  const findings =
      Array.isArray(data.findings)
          ? data.findings
          : [];


  document.getElementById(
      "findingTotal"
  ).textContent =
      findings.length;


  document.getElementById(
      "findingCountBadge"
  ).textContent =
      `${findings.length} ${
          findings.length === 1
              ? "finding"
              : "findings"
      }`;


  renderFindings(findings);


  /* Severity */

  document.getElementById(
      "criticalCount"
  ).textContent =
      Number(data.critical_count || 0);


  document.getElementById(
      "highCount"
  ).textContent =
      Number(data.high_count || 0);


  document.getElementById(
      "mediumCount"
  ).textContent =
      Number(data.medium_count || 0);


  document.getElementById(
      "lowCount"
  ).textContent =
      Number(data.low_count || 0);


  document.getElementById(
      "infoCount"
  ).textContent =
      Number(data.info_count || 0);


  /* Score explanation */

  document.getElementById(
      "scoreExplanation"
  ).textContent =
      data.score_explanation ||
      "Score calculated using the deterministic Code Quality Score Engine.";


  /* Categories */

  renderCategoryScores(
      data.category_scores || {}
  );


  /* Cached */

  const cachedBadge =
      document.getElementById(
          "cachedBadge"
      );


  if (data.cached) {

    cachedBadge.classList.remove(
        "hidden"
    );

  } else {

    cachedBadge.classList.add(
        "hidden"
    );
  }
}


/* =========================================================
   SCORE LABEL
   ========================================================= */

function getScoreLabel(score) {

  if (score >= 9) {
    return "Excellent";
  }

  if (score >= 8) {
    return "Very good";
  }

  if (score >= 7) {
    return "Good";
  }

  if (score >= 6) {
    return "Needs improvement";
  }

  if (score >= 4) {
    return "Needs attention";
  }

  return "High risk";
}


/* =========================================================
   SCORE RING
   ========================================================= */

function animateScoreRing(score) {

  const circle =
      document.getElementById(
          "scoreProgress"
      );


  if (!circle) {
    return;
  }


  const radius = 50;

  const circumference =
      2 * Math.PI * radius;


  circle.style.strokeDasharray =
      circumference;


  const safeScore =
      Math.max(
          0,
          Math.min(
              10,
              Number(score) || 0
          )
      );


  const progress =
      safeScore / 10;


  const offset =
      circumference *
      (1 - progress);


  circle.style.strokeDashoffset =
      circumference;


  requestAnimationFrame(() => {

    circle.style.strokeDashoffset =
        offset;
  });
}


/* =========================================================
   FINDINGS
   ========================================================= */

function renderFindings(findings) {

  const container =
      document.getElementById(
          "findings"
      );


  container.textContent = "";


  if (!findings.length) {

    const empty =
        document.createElement(
            "div"
        );

    empty.className =
        "empty-state";


    empty.textContent =
        "🎉 No significant findings were detected.";


    container.appendChild(
        empty
    );

    return;
  }


  findings.forEach(
      (finding, index) => {

        const card =
            document.createElement(
                "article"
            );

        card.className =
            "finding-card";


        const header =
            document.createElement(
                "div"
            );

        header.className =
            "finding-header";


        const left =
            document.createElement(
                "div"
            );

        left.className =
            "finding-header-left";


        const number =
            document.createElement(
                "span"
            );

        number.className =
            "finding-number";


        number.textContent =
            String(index + 1);


        const title =
            document.createElement(
                "h4"
            );


        title.textContent =
            finding.title ||
            "Code finding";


        left.appendChild(number);

        left.appendChild(title);


        const severity =
            document.createElement(
                "span"
            );


        const severityValue =
            String(
                finding.severity ||
                "info"
            ).toLowerCase();


        severity.className =
            `severity-badge ${severityValue}`;


        severity.textContent =
            severityValue.toUpperCase();


        header.appendChild(
            left
        );

        header.appendChild(
            severity
        );


        card.appendChild(
            header
        );


        /* Metadata */

        const metadata =
            document.createElement(
                "div"
            );

        metadata.className =
            "finding-meta";


        if (
            finding.line !== undefined &&
            finding.line !== null
        ) {

          const line =
              document.createElement(
                  "span"
              );

          line.textContent =
              `Line ${finding.line}`;

          metadata.appendChild(
              line
          );
        }


        if (finding.category) {

          const category =
              document.createElement(
                  "span"
              );

          category.textContent =
              finding.category;

          metadata.appendChild(
              category
          );
        }


        card.appendChild(
            metadata
        );


        /* Detail */

        if (finding.detail) {

          const detail =
              document.createElement(
                  "p"
              );

          detail.className =
              "finding-detail";

          detail.textContent =
              finding.detail;

          card.appendChild(
              detail
          );
        }


        /* Suggestion */

        if (finding.suggestion) {

          const suggestionBox =
              document.createElement(
                  "div"
              );

          suggestionBox.className =
              "suggestion-box";


          const suggestionTitle =
              document.createElement(
                  "strong"
              );

          suggestionTitle.textContent =
              "💡 Suggested improvement";


          const suggestion =
              document.createElement(
                  "p"
              );

          suggestion.textContent =
              finding.suggestion;


          suggestionBox.appendChild(
              suggestionTitle
          );

          suggestionBox.appendChild(
              suggestion
          );


          card.appendChild(
              suggestionBox
          );
        }


        /* Historical grounding */

        if (finding.grounded_rule_id) {

          const grounded =
              document.createElement(
                  "div"
              );

          grounded.className =
              "grounded-rule";


          grounded.textContent =
              `🧠 Learned rule #${finding.grounded_rule_id}`;


          card.appendChild(
              grounded
          );
        }


        container.appendChild(
            card
        );
      }
  );
}


/* =========================================================
   CATEGORY SCORES
   ========================================================= */

function renderCategoryScores(categories) {

  const container =
      document.getElementById(
          "categoryScores"
      );


  container.textContent = "";


  const entries =
      Object.entries(categories);


  if (!entries.length) {

    container.textContent =
        "No category scores available.";

    return;
  }


  entries.forEach(
      ([category, score]) => {

        const safeScore =
            Math.max(
                0,
                Math.min(
                    10,
                    Number(score) || 0
                )
            );


        const row =
            document.createElement(
                "div"
            );

        row.className =
            "category-row";


        const header =
            document.createElement(
                "div"
            );

        header.className =
            "category-row-header";


        const name =
            document.createElement(
                "span"
            );

        name.textContent =
            formatCategory(category);


        const value =
            document.createElement(
                "strong"
            );

        value.textContent =
            safeScore.toFixed(1);


        header.appendChild(name);

        header.appendChild(value);


        const track =
            document.createElement(
                "div"
            );

        track.className =
            "category-track";


        const fill =
            document.createElement(
                "div"
            );

        fill.className =
            "category-fill";


        fill.style.width =
            `${safeScore * 10}%`;


        track.appendChild(
            fill
        );


        row.appendChild(
            header
        );

        row.appendChild(
            track
        );


        container.appendChild(
            row
        );
      }
  );
}


/* =========================================================
   FORMAT CATEGORY
   ========================================================= */

function formatCategory(category) {

  return String(category)
      .replace(/[_-]/g, " ")
      .replace(
          /\b\w/g,
          character =>
              character.toUpperCase()
      );
}


/* =========================================================
   HISTORY
   ========================================================= */

async function loadHistory() {

  try {

    const headers =
        await getAuthHeaders();


    const response =
        await fetch(
            "/api/history",
            {
              method: "GET",
              headers
            }
        );


    if (!response.ok) {

      throw new Error(
          "History request failed."
      );
    }


    const data =
        await response.json();


    console.log(
        "History data:",
        data
    );


    renderHistory(
        data
    );


  } catch (error) {

    console.error(
        "History failed:",
        error
    );
  }
}


/* =========================================================
   RENDER HISTORY
   ========================================================= */

function renderHistory(data) {

  growth.classList.remove(
      "hidden"
  );


  const reviews =
      extractReviews(data);


  const scores =
      reviews
          .map(item =>
              Number(
                  item.rating ??
                  item.score ??
                  0
              )
          )
          .filter(score =>
              Number.isFinite(score)
          );


  document.getElementById(
      "reviewCount"
  ).textContent =
      reviews.length;


  if (scores.length) {

    const best =
        Math.max(...scores);


    document.getElementById(
        "bestScore"
    ).textContent =
        best.toFixed(1);


    if (scores.length >= 2) {

      const improvement =
          scores[scores.length - 1] -
          scores[0];


      const element =
          document.getElementById(
              "improvement"
          );


      element.textContent =
          `${improvement >= 0 ? "+" : ""}${improvement.toFixed(1)}`;

    } else {

      document.getElementById(
          "improvement"
      ).textContent =
          "First review";
    }


    document.getElementById(
        "learningPattern"
    ).textContent =
        scores.length >= 3
            ? "Growing"
            : "Learning";
  }


  renderTrendChart(
      reviews
  );


  renderHistoryList(
      reviews
  );
}


/* =========================================================
   EXTRACT REVIEWS
   ========================================================= */

function extractReviews(data) {

  if (Array.isArray(data)) {
    return data;
  }


  if (
      data &&
      Array.isArray(data.reviews)
  ) {
    return data.reviews;
  }


  if (
      data &&
      Array.isArray(data.history)
  ) {
    return data.history;
  }


  return [];
}


/* =========================================================
   TREND CHART
   ========================================================= */

function renderTrendChart(reviews) {

  const container =
      document.getElementById(
          "trendChart"
      );


  container.textContent = "";


  if (!reviews.length) {

    container.textContent =
        "Your score trend will appear after your first review.";

    return;
  }


  const recent =
      reviews.slice(-10);


  const scores =
      recent.map(
          item =>
              Number(
                  item.rating ??
                  item.score ??
                  0
              )
      );


  const max =
      Math.max(
          10,
          ...scores
      );


  const min =
      Math.min(
          0,
          ...scores
      );


  const chart =
      document.createElement(
          "div"
      );

  chart.className =
      "simple-chart";


  recent.forEach(
      (item, index) => {

        const score =
            Number(
                item.rating ??
                item.score ??
                0
            );


        const bar =
            document.createElement(
                "div"
            );

        bar.className =
            "chart-bar-wrapper";


        const value =
            document.createElement(
                "div"
            );

        value.className =
            "chart-value";


        value.textContent =
            score.toFixed(1);


        const track =
            document.createElement(
                "div"
            );

        track.className =
            "chart-bar-track";


        const fill =
            document.createElement(
                "div"
            );

        fill.className =
            "chart-bar-fill";


        const percentage =
            Math.max(
                5,
                Math.min(
                    100,
                    ((score - min) /
                        (max - min)) *
                    100
                )
            );


        fill.style.height =
            `${percentage}%`;


        track.appendChild(
            fill
        );


        const label =
            document.createElement(
                "span"
            );

        label.className =
            "chart-label";


        label.textContent =
            `${index + 1}`;


        bar.appendChild(
            value
        );

        bar.appendChild(
            track
        );

        bar.appendChild(
            label
        );


        chart.appendChild(
            bar
        );
      }
  );


  container.appendChild(
      chart
  );
}


/* =========================================================
   HISTORY LIST
   ========================================================= */

function renderHistoryList(reviews) {

  const container =
      document.getElementById(
          "historyList"
      );


  container.textContent = "";


  if (!reviews.length) {

    container.textContent =
        "No previous reviews yet.";

    return;
  }


  reviews
      .slice()
      .reverse()
      .slice(0, 10)
      .forEach(
          (review, index) => {

            const row =
                document.createElement(
                    "div"
                );

            row.className =
                "history-row";


            const info =
                document.createElement(
                    "div"
                );


            const language =
                review.language ||
                "Unknown";


            const date =
                review.created_at ||
                review.timestamp ||
                review.createdAt ||
                "";


            const title =
                document.createElement(
                    "strong"
                );


            title.textContent =
                LANGUAGE_NAMES[language] ||
                language;


            const meta =
                document.createElement(
                    "span"
                );


            meta.textContent =
                date
                    ? formatDate(date)
                    : `Review ${reviews.length - index}`;


            info.appendChild(
                title
            );

            info.appendChild(
                meta
            );


            const score =
                document.createElement(
                    "div"
                );


            score.className =
                "history-score";


            const numericScore =
                Number(
                    review.rating ??
                    review.score ??
                    0
                );


            score.textContent =
                Number.isFinite(
                    numericScore
                )
                    ? numericScore.toFixed(1)
                    : "—";


            row.appendChild(
                info
            );

            row.appendChild(
                score
            );


            container.appendChild(
                row
            );
          }
      );
}


/* =========================================================
   DATE
   ========================================================= */

function formatDate(value) {

  const date =
      new Date(value);


  if (
      Number.isNaN(
          date.getTime()
      )
  ) {
    return String(value);
  }


  return date.toLocaleString(
      undefined,
      {
        year: "numeric",
        month: "short",
        day: "numeric"
      }
  );
}


/* =========================================================
   EVENT LISTENERS
   ========================================================= */

loginButton.addEventListener(
    "click",
    login
);


heroLoginButton.addEventListener(
    "click",
    login
);


logoutButton.addEventListener(
    "click",
    logout
);


reviewButton.addEventListener(
    "click",
    reviewCode
);


/* Automatic language detection */

let detectionTimer = null;


codeInput.addEventListener(
    "input",
    () => {

      updateCharacterCount();


      clearTimeout(
          detectionTimer
      );


      detectionTimer =
          setTimeout(
              updateDetectedLanguage,
              250
          );
    }
);


/* Manual language selection */

languageSelect.addEventListener(
    "change",
    () => {

      languageSelect.classList.remove(
          "language-detected"
      );


      if (
          languageSelect.value === "auto"
      ) {

        languageHint.textContent =
            "Language will be detected automatically.";

      } else {

        languageHint.textContent =
            `Manual selection: ${
                LANGUAGE_NAMES[
                    languageSelect.value
                    ] ||
                languageSelect.value
            }`;
      }
    }
);


/* Keyboard shortcut */

codeInput.addEventListener(
    "keydown",
    event => {

      if (
          (event.metaKey ||
              event.ctrlKey) &&
          event.key === "Enter"
      ) {

        event.preventDefault();

        reviewCode();
      }
    }
);


/* Initial state */

updateCharacterCount();


console.log(
    "Intelligent Code Reviewer frontend loaded."
);

console.log(
    "Review button:",
    reviewButton
);

console.log(
    "Code input:",
    codeInput
);

console.log(
    "Language selector:",
    languageSelect
);