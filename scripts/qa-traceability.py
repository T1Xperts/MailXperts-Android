#!/usr/bin/env python3
"""Validate MailXperts SDLC/WBS inventory without claiming feature test execution.

This is traceability contract QA, not end-to-end AI or provider UAT.
Run: python3 scripts/qa-traceability.py
"""
from __future__ import annotations

import csv
import json
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "docs/traceability/WORK_BREAKDOWN_DEPENDENCIES.json"
RTM = ROOT / "docs/traceability/RTM.csv"
OUT = ROOT / "build/traceability/QA-RESULTS.json"
QA_PLAN = ROOT / "docs/traceability/QA_CASES_WAVES.json"
EXPECTED = {f"MX-QA-{n:03d}" for n in [29, 30]}
EXPECTED.update(f"AI-{n:03d}" for n in range(1, 37))
MUST_DRAFT_ONLY = {"draft_only", "no_automatic_post", "no_automatic_validate",
                   "no_automatic_approve", "no_automatic_pay",
                   "deduplicate", "audit_source"}


def validate():
    manifest = json.loads(SOURCE.read_text(encoding="utf-8"))
    with RTM.open(newline="", encoding="utf-8-sig") as stream:
        rtm_rows = list(csv.DictReader(stream))
    qa_plan = json.loads(QA_PLAN.read_text(encoding="utf-8"))
    problems = []
    checks = []

    def check(name, condition, message):
        checks.append({"test": name, "result": "PASS" if condition else "FAIL"})
        if not condition:
            problems.append(f"{name}: {message}")

    reqs = manifest["requirements"]
    ids = [r["id"] for r in reqs]
    rtm_ids = [r["Requirement_ID"] for r in rtm_rows]
    by_id = {r["id"]: r for r in reqs}
    by_rtm = {r["Requirement_ID"]: r for r in rtm_rows}
    pkgs = manifest["packages"]
    pkg_ids = {p["id"] for p in pkgs}

    check("RTM unique 66 IDs", len(rtm_rows) == 66 and len(set(rtm_ids)) == 66,
          f"Found {len(rtm_rows)} rows and {len(set(rtm_ids))} distinct IDs")
    check("Exactly 38 unfinished IDs", len(reqs) == len(set(ids)) == 38 and set(ids) == EXPECTED,
          "Unfinished inventory does not equal MX-QA-029/030 and AI-001..036")
    check("All unfinished IDs in master RTM", set(ids).issubset(set(rtm_ids)),
          "Unfinished IDs are absent from master RTM")
    check("Packages unique and complete",
          len(pkgs) == len(pkg_ids) == 10 and
          sorted(r for p in pkgs for r in p["requirements"]) == sorted(ids),
          "Every unfinished requirement must occur in exactly one package")
    check("Package size/WBS assignments",
          all(p["size"] == len(p["requirements"]) and
              all(by_id[x]["package_id"] == p["id"] for x in p["requirements"])
              for p in pkgs),
          "Package membership/size mismatch")
    check("All seven waves accounted for",
          Counter(r["wave"] for r in reqs) == {0: 5, 1: 7, 2: 9, 3: 5, 4: 6, 5: 5, 6: 1},
          "Wrong wave sizes or missing wave")
    check("Dependencies internal and ordered",
          all(d["id"] in by_id and d["id"] != r["id"] and
              by_id[d["id"]]["wave"] <= r["wave"]
              for r in reqs for d in r["internal_dependencies"]),
          "Dependency missing, self-referential or in a later wave")
    states = {}
    cycles = []

    def visit(node):
        if states.get(node) == 1:
            cycles.append(node)
            return
        if states.get(node) == 2:
            return
        states[node] = 1
        for edge in by_id[node]["internal_dependencies"]:
            if edge["id"] in by_id:
                visit(edge["id"])
        states[node] = 2

    for node in ids:
        visit(node)
    check("No dependency cycles", not cycles, f"Cycle(s): {cycles}")
    internals = sum(bool(r["internal_dependencies"]) for r in reqs)
    externals = sum(bool(r["external_prerequisites"]) for r in reqs)
    overlap = sum(bool(r["internal_dependencies"]) and bool(r["external_prerequisites"]) for r in reqs)
    check("Dependence classification totals", (internals, externals, overlap) == (33, 14, 12),
          f"Observed internal={internals}, external={externals}, overlap={overlap}")
    check("Documented external gates not conflated with proposals",
          sum(any(x["evidence_state"] == "documented_pending" for x in r["external_prerequisites"])
              for r in reqs) == 8 and
          all(x["evidence_state"] in ("documented_pending", "proposed")
              for r in reqs for x in r["external_prerequisites"]),
          "External gate evidence count/status invalid")
    check("Odoo draft-only accounting guardrails",
          MUST_DRAFT_ONLY.issubset(set(by_id["AI-036"]["safety_controls"])),
          "Never permit automatic Odoo posting, validating, approval or payment")
    check("BillPay must require human verification",
          {"explicit_user_confirmation", "strong_identity_check",
           "no_automatic_payment"}.issubset(set(by_id["AI-034"]["safety_controls"])),
          "BillPay approval boundary missing")
    check("AI reply no auto-send",
          "no_automatic_send" in by_id["AI-002"]["safety_controls"],
          "Drafting must not send automatically")
    fields = {"WBS_Package", "Execution_Wave", "Internal_Dependencies",
              "External_Prerequisite_Readiness", "Design_Approval_Status",
              "Automated_QA_Execution_Status", "UAT_Gate_Status", "Production_Release_Gate",
              "QA_Plan_Case_IDs", "QA_Plan_Definition_Link"}
    check("Extended RTM fields present", all(fields.issubset(row) for row in rtm_rows),
          "Extended RTM columns missing")
    check("RTM matches dependency/WBS manifest",
          all(by_rtm.get(r["id"], {}).get("WBS_Package") == r["package_id"] and
              by_rtm.get(r["id"], {}).get("Execution_Wave") == str(r["wave"]) and
              by_rtm.get(r["id"], {}).get("Internal_Dependencies", "") ==
                  "; ".join(x["id"] for x in r["internal_dependencies"])
              for r in reqs),
          "RTM wave/package/dependency columns out of sync")
    planned = qa_plan["cases"]
    expected_cases = {f"TC-{r['id']}-{n:02d}"
                      for r in reqs for n in range(1, 4)}
    check("114 QA scenarios defined exactly once",
          len(planned) == qa_plan["case_count"] == 114 and
          len({c["id"] for c in planned}) == 114 and
          {c["id"] for c in planned} == expected_cases,
          "Expected three planned cases for each of the 38 unfinished requirements")
    check("QA plans do not falsely claim execution",
          all(c["status"] == "PLANNED_NOT_RUN" and not c["evidence_url"]
              for c in planned),
          "Proposed test cases must not be marked as executed or passed")
    check("QA layer breakdown",
          Counter(c["test_layer"] for c in planned) == {
              "automated_functional_or_contract": 38,
              "automated_negative_security_regression": 38,
              "device_or_provider_UAT": 38},
          "Missing functional, negative/security or UAT test case")
    check("Every unfinished requirement links all three planned QA cases",
          all(by_rtm.get(r["id"], {}).get("QA_Plan_Case_IDs", "") ==
              "; ".join(f"TC-{r['id']}-{n:02d}" for n in range(1, 4))
              and by_rtm.get(r["id"], {}).get("QA_Plan_Definition_Link", "")
              for r in reqs),
          "Test plan links or the 3 QA-case IDs do not match the manifest")
    check("No unfinished feature falsely marked as production released",
          all("RELEASED" not in by_rtm.get(r["id"], {}).get("Production_Release_Gate", "").upper()
              or by_rtm.get(r["id"], {}).get("Production_Release_Gate") == "NOT_RELEASED"
              for r in reqs),
          "An unfinished feature appears to be production released")

    result = {
        "kind": "traceability_contract_QA_only",
        "scope": {"rtm_total": len(rtm_rows), "unfinished": len(reqs),
                  "internal": internals, "external": externals, "overlap": overlap, "planned_cases": len(planned)},
        "result": "PASS" if not problems else "FAIL",
        "checks": checks, "errors": problems,
        "limitations": [
            "No Android unit tests executed by this script.",
            "No server/hosting/provider/live API integration was exercised.",
            "No physical-device UAT or human design approval is implied.",
            "Unimplemented feature test cases remain NOT RUN."
        ]
    }
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(result, indent=2))
    if problems:
        raise SystemExit(1)


if __name__ == "__main__":
    validate()
