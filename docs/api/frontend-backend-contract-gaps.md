# Frontend and Backend Contract Gaps

Audit date: 2026-06-19

This document records contract differences found between the current frontend
mock API surface and the public backend/OpenAPI contract. It is an input for
product confirmation, not an instruction to change frontend or backend behavior
immediately.

Do not resolve these gaps automatically. Confirm the desired product/API shape
first, then update the OpenAPI contract, backend implementation, frontend mock,
and client integration together.

## Gap Summary

| ID | Area | Current Frontend Mock Surface | Current Backend/OpenAPI Surface | Confirmation Needed |
| --- | --- | --- | --- | --- |
| GAP-001 | Auth | Guest-session flow is represented in the mock client. | `/api/auth/social` exists for Google/Apple ID-token sessions. | Confirm when the frontend mock/client should add `createSocialSession` and remove or hide guest-only assumptions outside local validation. |
| GAP-002 | Comments | Comment list/create methods are not part of the current mock client surface. | `GET /api/rooms/{roomId}/responses/{responseId}/comments` and `POST /api/rooms/{roomId}/responses/{responseId}/comments` exist. | Confirm whether comments should enter the frontend mock/client now or remain out of the near-term app flow. |
| GAP-003 | Room membership | Owner member removal exists in the mock client surface. | Backend/OpenAPI also expose `DELETE /api/rooms/{roomId}/members/me` for member self-leave. | Confirm whether the frontend needs a self-leave flow before ownership transfer is supported. |
| GAP-004 | Room summaries | Room summary assumes `lastMissionDate` is always present and does not model `today`. Room detail also does not model `today`. | `lastMissionDate` is nullable, and `today` is nullable on room summary/detail responses. | Confirm whether frontend types should accept `lastMissionDate: null` and consume `today`, or whether the backend should keep these fields out of early integration responses. |
| GAP-005 | Video asset metadata | Video assets assume `thumbnailUrl`, `width`, `height`, and `fileSizeBytes` are always present. | Backend/OpenAPI allow these fields to be nullable. | Confirm whether the backend will guarantee these values after upload verification, or whether frontend playback surfaces must handle missing thumbnails and metadata. |
| GAP-006 | Today previews | The mock today response includes `todayFrames` and `growthPreview` data. | Backend `GET /api/rooms/{roomId}/today` currently returns empty preview arrays, while wall and growth data are available through separate endpoints. | Confirm whether today should stay lean and clients should call wall/growth endpoints separately, or whether backend should populate these preview arrays. |
| GAP-007 | Growth reward set | The mock uses reward types such as `WEIRD_OBJECT_SHELF`, `MOOD_WINDOW`, `TICKET_WALL`, and `ROOM_NAMEPLATE`. | Backend currently returns `ROOM_NAMEPLATE`, `FRIDGE_MAGNET`, and `MONTHLY_FRAME`; OpenAPI lists a larger superset. | Confirm the MVP reward enum and initial reward definitions before wiring real frontend surfaces to backend data. |

## Recommended Resolution Order

1. Resolve GAP-001 with the real Google/Apple client-id setup and frontend auth
   handoff.
2. Resolve GAP-004 and GAP-005 before replacing mock room/today data with live
   backend responses, because these can cause runtime null-handling issues.
3. Resolve GAP-006 and GAP-007 when deciding how much wall/growth data the today
   screen should fetch directly.
4. Resolve GAP-002 and GAP-003 when comments and self-leave become active UI
   flows.

## Non-Gaps Observed

- Core room, invite, today mission, mission edit, upload-url, response create,
  response delete, reaction, wall, growth, and owner remove-member operation
  names are aligned between the current mock client surface and OpenAPI.
- The backend exposes OpenAPI at `/api-docs/openapi.yaml`, so future integration
  work can verify the deployed contract without reading repository files.
