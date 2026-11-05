# Parking lot LLD

Start with [the rendered study guide](STUDY_GUIDE.html), or read [the Markdown edition](STUDY_GUIDE.md). The HTML includes 12 UML diagrams with click-to-enlarge controls and expandable source code, and works offline.

Four standalone Java 17+ versions build the design progressively:

1. `checkpoint1/BasicParkingDemo.java`: compatible allocation and release.
2. `checkpoint2/TicketParkingDemo.java`: tickets, hourly billing and repeatable exit.
3. `checkpoint3/PolicyParkingDemo.java`: interchangeable allocation and pricing strategies.
4. `checkpoint4/PaymentParkingDemo.java`: payment lifecycle, retries and concurrent gates.

From the repository root:

```powershell
java --source 17 case-studies/parking-lot/phases/checkpoints/checkpoint1/BasicParkingDemo.java
java --source 17 case-studies/parking-lot/phases/checkpoints/checkpoint2/TicketParkingDemo.java
java --source 17 case-studies/parking-lot/phases/checkpoints/checkpoint3/PolicyParkingDemo.java
java --source 17 case-studies/parking-lot/phases/checkpoints/checkpoint4/PaymentParkingDemo.java
```

The linked YouTube video returned a members-only restriction during research. The document identifies this limitation and does not claim to reproduce its inaccessible content.

To update the guide after editing Java, run `python case-studies/parking-lot/phases/checkpoints/tools/build-guide.py`. The template contains prose, Mermaid and code insertion markers. The generated Markdown embeds the exact runnable sources; the HTML additionally embeds SVG diagrams.

Rendering also needs Node 22.13+ (verified with Node 24.22.1), Mermaid CLI 12.0.0, `marked`, and Chrome. Adjust `tools/puppeteer.json` if Chrome is elsewhere. With a compatible Node on PATH:

```powershell
$env:PUPPETEER_SKIP_DOWNLOAD = 'true'
npm exec --yes --package=@mermaid-js/mermaid-cli@12.0.0 -- mmdc -i case-studies/parking-lot/phases/checkpoints/STUDY_GUIDE.md -o case-studies/parking-lot/phases/checkpoints/STUDY_GUIDE.rendered.md -e svg -p case-studies/parking-lot/phases/checkpoints/tools/puppeteer.json -c case-studies/parking-lot/phases/checkpoints/tools/mermaid.json -b transparent
node case-studies/parking-lot/phases/checkpoints/tools/render-guide.mjs '<absolute node_modules directory containing marked>'
```

`tools/check-guide.mjs` accepts a node_modules directory containing Playwright, renders the local HTML in Chrome, and checks the 12 diagrams, four Java blocks, navigation, diagram enlargement, code expansion and mobile overflow. Its screenshots are in `verification/`.

All four Java examples passed using Java 22.0.1 in Java 17 source mode. These are in-memory study implementations; real payments, persistence and hardware are discussed as extensions.
