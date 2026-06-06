import { cp, mkdir, readFile, writeFile } from "node:fs/promises";

const source = "src/main/resources/static";
const output = "dist";
const apiUrl = (process.env.ASAMI_API_URL || "").replace(/\/$/, "");

await mkdir(output, { recursive: true });
await cp(source, output, { recursive: true });

const config = `window.ASAMI_API_URL = ${JSON.stringify(apiUrl)};\n`;
await writeFile(`${output}/config.js`, config, "utf8");

const index = await readFile(`${output}/index.html`, "utf8");
await writeFile(
  `${output}/index.html`,
  index.replace(
    '<script src="/app.js"></script>',
    '<script src="/config.js"></script><script src="/app.js"></script>'
  ),
  "utf8"
);
