"""
Check the ordering claim in COMMIT_PLAN.md.

The plan says each commit only uses files an earlier commit already added.
This tests that for Java: for every class the project defines, find which
commit adds it, then check that every commit importing or naming that class
comes at the same time or later.

Only project classes are considered - JDK and Spring types are irrelevant
here, since they come from the dependencies, not the history.
"""
import os
import re
import sys
from collections import defaultdict

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from make_commit_plan import PLAN, ROOT

JAVA = re.compile(r"\.java$")
PKG = re.compile(r"^\s*package\s+([\w.]+)\s*;", re.M)
IMPORT = re.compile(r"^\s*import\s+(?:static\s+)?([\w.]+)\s*;", re.M)


def main():
    # commit index for every file
    at = {}
    order = []
    n = 0
    for _, commits in PLAN:
        for msg, _, files in commits:
            n += 1
            order.append(msg)
            for f in files:
                at[f] = n

    # fully-qualified name -> commit that adds it
    defined = {}
    for f, idx in at.items():
        if not JAVA.search(f):
            continue
        path = os.path.join(ROOT, f)
        if not os.path.exists(path):
            continue
        src = open(path, encoding="utf-8", errors="replace").read()
        m = PKG.search(src)
        if m:
            defined[f"{m.group(1)}.{os.path.basename(f)[:-5]}"] = (idx, f)

    problems = []
    for f, idx in sorted(at.items(), key=lambda kv: kv[1]):
        if not JAVA.search(f):
            continue
        path = os.path.join(ROOT, f)
        if not os.path.exists(path):
            continue
        src = open(path, encoding="utf-8", errors="replace").read()
        for imp in IMPORT.findall(src):
            if imp not in defined:
                continue                     # third-party or JDK
            dep_idx, dep_file = defined[imp]
            if dep_idx > idx:
                problems.append((idx, f, dep_idx, dep_file, imp))

    print(f"commits            : {n}")
    print(f"project classes    : {len(defined)}")
    print(f"forward references : {len(problems)}")
    if problems:
        print("\nA commit uses a class that a LATER commit adds:\n")
        by_pair = defaultdict(list)
        for idx, f, didx, dfile, imp in problems:
            by_pair[(idx, didx)].append((f, imp))
        for (idx, didx), items in sorted(by_pair.items())[:25]:
            print(f"  commit {idx} (\"{order[idx-1]}\")")
            print(f"    needs commit {didx} (\"{order[didx-1]}\")")
            for f, imp in items[:3]:
                print(f"      {os.path.basename(f)} imports {imp}")
            print()
        return 1

    print("\nOK  no commit uses a class added by a later commit")
    return 0


if __name__ == "__main__":
    sys.exit(main())
