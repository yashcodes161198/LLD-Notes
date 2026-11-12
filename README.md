# LLD notes

Java examples, design pattern notes, and low-level design case studies.

```text
LLD-Notes/
├── patterns/
│   ├── Strategy/
│   ├── Observer/
│   └── Decorator/
├── case-studies/
│   ├── amazon-locker/
│   ├── bookmyshow/
│   ├── job-scheduler/
│   │   └── hello-interview-bookmyshow/
│   ├── llm-usage-limits/
│   ├── notification-router/
│   ├── parking-lot/
│   ├── rate-limiter/
│   └── splitwise/
└── resources/
```

## Design patterns

- [Strategy](patterns/Strategy/README.md): interchangeable duck behaviors.
- [Observer](patterns/Observer/README.md): push and pull weather updates.
- [Decorator](patterns/Decorator/README.md): composing coffee orders.

Each pattern includes a README and standalone `bad/` and `good/` Java examples. Run the commands in its README from this repository's root.

## Case studies

- [Amazon locker](case-studies/amazon-locker/README.md): interview-prep format (annotated solution, 5 workbooks, answer keys, rulebook)
- [BookMyShow](case-studies/bookmyshow/README.md): phased Java notes, plus the [interview-prep format](case-studies/bookmyshow/hello-interview-bookmyshow/README.md) (annotated solution, 5 workbooks, answer keys, rulebook, Java code)
- [Job scheduler](case-studies/job-scheduler/README.md): interview-prep format (annotated solution, 5 workbooks, answer keys, rulebook, Java code)
- [LLM usage limits](case-studies/llm-usage-limits/README.md): 5-hour + weekly quota with unknown request cost and parallel chats; interview-prep format (annotated solution, 5 workbooks, answer keys, rulebook, Java code)
- [Notification router](case-studies/notification-router/README.md): interview-prep format (annotated solution, 5 workbooks, answer keys, rulebook)
- [Parking lot](case-studies/parking-lot/README.md)
- [Rate limiter](case-studies/rate-limiter/README.md): phased Java notes, plus the interview-prep format (annotated solution, 5 workbooks, answer keys, rulebook)
- [Splitwise](case-studies/splitwise/README.md)

Every case study follows the same layout:

```text
<case-study>/
├── README.md       Study roadmap and run instructions
├── notes/          PDF study notes
├── phases/         Complete Java implementation for each phase
├── diagrams/       Class and sequence diagrams
└── screenshots/    Reference screenshots
```

Start with the case study's README and PDF, then work through its phases. Each phase includes a `run.sh` and needs JDK 17 or newer. Run phase commands from the case study's folder, for example:

```bash
cd case-studies/parking-lot
bash phases/Phase-1-Vehicles-and-Spots/run.sh
```

Parking lot also includes an [alternative checkpoint guide](case-studies/parking-lot/phases/checkpoints/README.md), with four standalone examples and an offline HTML guide.

## Reference material

The [resources folder](resources/) contains the Head First Design Patterns reference PDF. IDE settings, macOS metadata, and compiled Java output are ignored by Git.
