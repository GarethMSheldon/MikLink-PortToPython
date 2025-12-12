# MikLink — Testing Strategy

This document summarizes the testing strategy for MikLink and the way golden fixtures
are used for deterministic parsing/contract tests.

Pyramid:
- Domain unit tests (core/domain) — logic contracts
- Data integration / parsing (core/data) — golden fixtures
- ViewModel minimal tests — minimal mapping from domain to view state
- UI instrumentation: minimal or none in this EPIC

Golden fixtures RouterOS:
- Fixtures live in `app/src/test/resources/fixtures/routeros/7.20.5/`
- They are copies of outputs collected from real routers using the commands below
- Tests assert parsing correctness; when parsing fails, update DTO/mappers, do not change fixtures

Commands used to collect fixtures:
- `curl -k --max-time 10 -u "$USER:$PASS" "https://$ROUTER/rest/system/resource?.proplist=.id,time,topics,message"`
- `curl -k --max-time 10 -u "$USER:$PASS" "https://$ROUTER/rest/ip/neighbor"`
- `curl -k --max-time 10 -u "$USER:$PASS" "https://$ROUTER/rest/interface/ethernet/monitor?interface=ether1"`
- `curl -k --max-time 10 -u "$USER:$PASS" "https://$ROUTER/rest/interface/ethernet/cable-test?interface=ether1"`
- `curl -k --max-time 10 -u "$USER:$PASS" "https://$ROUTER/rest/interface/bridge/host"`
- `curl -k --max-time 10 -u "$USER:$PASS" "https://$ROUTER/rest/interface/bridge/port"`

Fixtures sensitive notes:
- `bridge_port.json` contains debug-info (multiline) and must be left unmodified
- `log_get_proplist.json` is a long array and used for log filtering tests

Legacy tests
- Legacy test suites were removed in EPIC T1 and replaced with golden fixtures (core/data) and contract tests (core/domain)

Test running
- Local tests are executed via the existing gradle task `./gradlew testDebugUnitTest` (or `./gradlew test` depending on modules). No CI changes in this EPIC.

TODOs:
- Add small fixtures for more RouterOS outputs as integrations increase
- Align `TestMoshiProvider` to production once DI modules are stabilized
