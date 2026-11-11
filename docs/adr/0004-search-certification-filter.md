# 0004: Search sends the certification filter, which TMDb ignores there

**Status:** Accepted (requirements Q2; implemented in T0.5 and T3.2)

## Context
The task says every TMDb request must include `include_adult=false`, `certification_country=US` and `certification.lte=PG-13` (R24–R26), so only movies rated G, PG or PG-13 appear. TMDb applies the two certification parameters only on `/discover/movie`. `/search/movie` and `/movie/{id}` accept them but ignore them; on search, only `include_adult` has an effect.

## Decision
- **One OkHttp interceptor adds all three parameters to every request**, including search, exactly as the task requires. Tests assert that they are present on discover and search requests.
- **The popular list uses `/discover/movie`**, where the full filter applies.
- **Search uses `/search/movie`** and therefore filters only adult content. This is documented as a known limitation here, in the README and in the search feature doc.

## Alternatives considered
- **Filter search results on the client by certification.** TMDb's search response has no certification. Getting it needs one extra request per movie (`/movie/{id}/release_dates`), which multiplies traffic and latency for every keystroke-driven search.
- **Search through `/discover/movie`.** Discover has no free-text title parameter (only keyword and genre IDs), so it cannot implement "search by title" (R12).
- **Drop the parameters from search requests.** That would break the "every request" requirement for no gain.

## Consequences
- **Search may show some titles rated above PG-13.** The popular list never does.
- **The requirement and its limit are explicit and tested,** so a reviewer can see the behavior was a deliberate choice, not an oversight.
