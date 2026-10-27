# Prior-build diagnostic runs

These two fresh model evaluations used the same older immutable application package. Their paired model inputs were consistent with each other, but the package lacked the current normalization of human-facing room messages into zero-based model identities. A source-hash-only check initially missed this mismatch.

The issue was caught before final delivery. Both models were rerun on the byte-verified current package; see the parent directory for the accepted comparison. These earlier action records and one unmodified native replay are retained for transparency and must not be presented as current-build results. The source hashes in these files describe the then-current checkout, not the serving package.

The old paired outcomes were 0/9 escapes for 0.8B and 1/9 for 4B. Maze mechanics, seeds and initial observations were unchanged; post-move result text differed. No model failure was discarded or replaced with a solver move.
