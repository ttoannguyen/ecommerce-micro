---
name: handoff
description: Write or refresh HANDOFF.md — the document that lets someone (or future-you) pick this repo up cold. Use when the user says "handoff", "viết handoff", "write handoff doc", "document where we are", or at the end of a big chunk of work.
---

# Handoff doc

Produce `HANDOFF.md` at the repo root. It answers one question: **what would someone need to
know to continue this work tomorrow, that they cannot get from reading the code?**

## Rules

1. **Do not restate the code.** Package trees, class lists, "OrderController handles /orders" —
   the reader can see that. Every line must carry information the repo does not already record.
2. **Every decision gets a "why".** Not "we use pessimistic locking" but "pessimistic, because
   stock is one hot row and optimistic would make 19 of 20 requests retry".
3. **Write down what you got wrong.** The bug you hit and fixed is worth more than the design
   that worked first try. Someone will re-introduce it otherwise.
4. **Known debt is a table, and it is honest.** Each row: the debt, why it bites, how to fix.
   Do not hide the things that are broken — that is the whole point of the document.
5. **Environment traps.** Anything that cost you more than 10 minutes to figure out: missing
   toolchain, encoding, exec bits, version pins that lie.
6. **Verify before you claim.** Do not write "all tests pass" without running them. If something
   is unverified, say so in the document.

## Structure

```
# Handoff — <repo>
Date, branch, remote.

## Đang ở đâu / Where we are      <- state + how to run it in 30 seconds
## Quyết định kiến trúc           <- each decision + WHY + what it changed in the code
## Nợ kỹ thuật đã biết            <- table: debt | why it hurts | how to fix
## Lộ trình                       <- what comes next
## Bẫy môi trường                 <- traps that cost time
```

## Language

Match the language of the repo's other docs (this one: Vietnamese prose, English code comments).
