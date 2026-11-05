# Verification record

Date: 3 November 2026.

- All six phases compiled with `javac --release 17 -encoding UTF-8` and executed successfully on installed OpenJDK 22.0.1. No claim is made that a JDK 17 runtime was installed or used.
- Each expected-output.txt is actual captured output from its matching Main. The PDF reads those same captured outputs.
- Phase 6 passes 17 executable assertions, including two-thread confirmation and a controlled expiry-during-payment interleaving.
- Consecutive phase source differences were generated and inspected; the Phase 6 change boundary is limited to the lock interface/provider, booking repository/service, Main and SafetyChecks.
- Java sources have no third-party dependencies. Both supplied launchers were checked using a scratch copy.
- PDF: 55 pages, nine A4 landscape pages. Every page was rendered with Poppler and visually reviewed. All PDF fonts are embedded. No code-line overflow remained in the final HTML layout check.
- Seven class maps and two sequence SVGs were rendered and reviewed. Class positions are stable across phases. Selected member lists are intentional; the diagrams omit utility harness classes, the custom exception and unused SeatStatus.
- Contents and README page ranges were extracted from the rendered PDF and checked after rebuilding. Code snippets are read from actual phase files; line-change highlighting comes from difflib.
- Sixteen full-resolution source frames are included and attributed. No Redis connection or real payment was executed.

## Format memory entry: lld-study-pack-format

2026-11-03: BookMyShow-LLD-Study. Six cumulative Java 17 phases; 55-page PDF; 16 source frames; seven class maps plus two sequences; Windows and Bash run scripts; actual outputs and 17 final-phase safety checks. The matching author repository package was unavailable, so video reconstruction is labeled explicitly. Phase 6 adds single-process booking safety; Redis is discussed, not implemented. This is a local record, not an update to an external memory service.
