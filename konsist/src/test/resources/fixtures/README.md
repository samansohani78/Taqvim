# Konsist fixtures

Kotlin sources in these trees are **never compiled**. They mimic the Taqvim module layout so
`KonsistFixturesTest` can prove that every architecture rule (T-002) flags planted violations
(`violations/`) and accepts compliant code (`compliant/`). They are excluded from detekt and from
every real-project Konsist scope.
