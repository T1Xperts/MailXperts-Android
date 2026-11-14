#!/usr/bin/env python3
"""Validate the MailXperts planning baseline only. Never touches servers, mailboxes or signing."""
from __future__ import annotations
import csv
from collections import Counter, defaultdict, deque
from pathlib import Path
from datetime import date

ROOT = Path(__file__).resolve().parents[1]
R = ROOT / "docs" / "release"
T = ROOT / "docs" / "traceability"

def read(path):
    with path.open(encoding="utf-8-sig", newline="") as f:
        return list(csv.DictReader(f))

def check(condition, message):
    if not condition:
        raise AssertionError(message)

def validate():
    rtm = read(T / "RTM.csv")
    scope = read(R / "P0P1_RELEASE_SCOPE_2026-11-14.csv")
    backlog = read(T / "MXAI_PLUGIN_BACKLOG.csv")
    schedule = read(R / "P0P1_PRODUCTION_EXECUTION_SCHEDULE_2026-11-14.csv")
    dependencies = read(R / "P0P1_EXECUTION_DEPENDENCIES_2026-11-14.csv")
    changes = read(R / "P0P1_DEPENDENCY_REPAIRS_2026-11-14.csv")
    packages = read(R / "P0P1_WORK_PACKAGES_2026-11-14.csv")

    check(len(rtm) == 160, "Historical RTM must contain 160 IDs")
    check(len(rtm[0]) == 44, "The source RTM must retain its 44-column contract")
    expected = {r["Requirement_ID"] for r in rtm if r["Priority"] in ("P0", "P1")}
    got = [r["Requirement_ID"] for r in scope]
    check(len(expected) == 131 and len(got) == 131 and set(got) == expected,
          "The release scope must match all 131 original P0/P1 IDs exactly")
    check(len(got) == len(set(got)), "Duplicate scoped requirement")
    check(Counter(r["Priority"] for r in scope) == {"P0": 70, "P1": 61},
          "P0/P1 priority counts changed without formal scope change")
    check(len(backlog) == 94 and len({r["ID"] for r in backlog}) == 94,
          "Expected 94 distinct MXAI backlog IDs")
    check(len(changes) == 9, "Expected nine auditable dependency repairs")
    check(len(packages) == 16, "Expected 16 resource-accountable work packages")

    bm = {r["ID"]: r for r in backlog}
    for row in backlog:
        for dep in filter(None, (z.strip() for z in row["Dependencies"].split(","))):
            check(dep in bm, f"Unknown MXAI dependency: {row['ID']} -> {dep}")
            check(int(bm[dep]["Wave"][1:]) <= int(row["Wave"][1:]),
                  f"Forward-wave source dependency: {row['ID']} -> {dep}")
    for row in changes:
        check(bm[row["ID"]]["Dependencies"] == row["Proposed_Execution_Dependencies"],
              f"Repair ledger and source disagree: {row['ID']}")

    ids = [r["Task_ID"] for r in schedule]
    all_ids = set(ids)
    check(len(schedule) == 164 and len(all_ids) == 164, "Expected 164 unique execution tasks")
    required = {r["Original_Requirement_ID"] for r in schedule if r["Original_Requirement_ID"]}
    check(required == expected, "Every requirement must have exactly one scheduled task")
    check(len(packages) == len({r["Work_Package"] for r in packages}),
          "Work package IDs must be distinct")
    for t in schedule:
        check(t["Proposed_Human_Accountable"] and t["Proposed_AI_Assistant"],
              f"Missing resource assignment: {t['Task_ID']}")
        check(t["Planned_Start"] <= t["Planned_Finish"],
              f"Start/finish inverted: {t['Task_ID']}")
        check(t["Implementation_State"] == "NOT_COMPLETED_AS_PART_OF_THIS_PLAN",
              f"Planning PR cannot claim new implementation: {t['Task_ID']}")

    check(len(dependencies) >= 500, "Expected full all-ID dependency network")
    indegree = {k: 0 for k in all_ids}
    graph = defaultdict(list)
    keys = set()
    rowmap = {r["Task_ID"]: r for r in schedule}
    for e in dependencies:
        pre, suc = e["Predecessor_ID"], e["Successor_ID"]
        check(pre in all_ids and suc in all_ids and pre != suc,
              f"Invalid dependency endpoint: {pre} -> {suc}")
        check(e["Relationship"] in ("FS", "SS", "FF", "SF"),
              f"Unsupported dependency type: {e['Relationship']}")
        key = (pre, suc, e["Relationship"], e["Lag_Business_Days"])
        check(key not in keys, f"Duplicate dependency {key}")
        keys.add(key)
        graph[pre].append(suc)
        indegree[suc] += 1
        p, s = rowmap[pre], rowmap[suc]
        if e["Relationship"] == "FS":
            check(p["Planned_Finish"] < s["Planned_Start"],
                  f"Finish-to-start conflict: {pre} -> {suc}")
        elif e["Relationship"] == "SS":
            check(p["Planned_Start"] <= s["Planned_Start"],
                  f"Start-to-start conflict: {pre} -> {suc}")
        elif e["Relationship"] == "FF":
            check(p["Planned_Finish"] <= s["Planned_Finish"],
                  f"Finish-to-finish conflict: {pre} -> {suc}")
        else:
            check(p["Planned_Start"] <= s["Planned_Finish"],
                  f"Start-to-finish conflict: {pre} -> {suc}")

    ready = deque(k for k, v in indegree.items() if v == 0)
    visited = 0
    while ready:
        u = ready.popleft()
        visited += 1
        for v in graph[u]:
            indegree[v] -= 1
            if indegree[v] == 0:
                ready.append(v)
    check(visited == len(all_ids), "Cycle detected in full production network")
    for item in expected:
        check((item, "PLAY-005", "FS", "0") in keys,
              f"Missing complete-implementation-to-signed-APK gate for {item}")
        check((item, "REL-001", "FS", "0") in keys,
              f"Missing individual acceptance-to-release gate for {item}")
    for predecessor, successor in [
        ("PLAY-005", "QA-003"), ("QA-003", "QA-004"), ("QA-004", "PLAY-007"),
        ("PLAY-007", "PLAY-008"), ("PLAY-008", "REL-001"),
        ("REL-001", "REL-003"), ("REL-003", "REL-004"),
    ]:
        check(any(e["Predecessor_ID"] == predecessor and
                  e["Successor_ID"] == successor and e["Relationship"] == "FS"
                  for e in dependencies),
              f"Mandatory release gate missing: {predecessor} -> {successor}")
    types = Counter(e["Relationship"] for e in dependencies)
    check(set(types) == {"FS", "SS", "FF", "SF"},
          "All four dependency relation types must be represented")
    for tid in ("PLAY-001", "PLAY-002", "PLAY-003", "PLAY-004",
                "PLAY-005", "PLAY-006", "PLAY-007", "PLAY-008",
                "REL-001", "REL-003", "REL-004"):
        check(tid in all_ids, f"Play/release work package missing: {tid}")
    print(f"PASS: {len(expected)} P0/P1 IDs, {len(schedule)} tasks, "
          f"{len(dependencies)} links, {len(packages)} WPs, DAG; "
          f"dependency types {dict(types)}.")
    print("NOTE: planning-data checks only. No build/security/provider/device UAT has run.")

if __name__ == "__main__":
    validate()
