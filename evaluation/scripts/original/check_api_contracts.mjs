import fs from 'node:fs';
import path from 'node:path';

const verifyRoot = '/workspace/material-system/agent-eval/verification/run-01';
const candidates = ['mini', 'dsh', 'pi'];

function walk(root, predicate) {
  const result = [];
  for (const entry of fs.readdirSync(root, { withFileTypes: true })) {
    const full = path.join(root, entry.name);
    if (entry.isDirectory()) result.push(...walk(full, predicate));
    else if (predicate(full)) result.push(full);
  }
  return result;
}

function normalizeBackendPath(value) {
  const normalized = (`/${value}`).replaceAll('//', '/').replace(/\{[^}]+\}/g, ':param');
  return normalized.length > 1 && normalized.endsWith('/') ? normalized.slice(0, -1) : normalized;
}

function backendEndpoints(root) {
  const controllers = walk(path.join(root, 'backend/src/main/java'),
    (file) => file.includes('/controller/') && file.endsWith('.java'));
  const endpoints = [];

  for (const file of controllers) {
    const source = fs.readFileSync(file, 'utf8');
    const classIndex = source.search(/\bclass\s+\w+/);
    const classPrefix = source.slice(0, classIndex < 0 ? 0 : classIndex)
      .match(/@RequestMapping\s*\(\s*(?:(?:value|path)\s*=\s*)?["']([^"']+)["']/)?.[1] ?? '';
    const mapping = /@(Get|Post|Put|Delete|Patch)Mapping\s*(?:\(\s*(?:(?:value|path)\s*=\s*)?["']([^"']*)["'][^)]*\)|\(\s*\)|)/g;
    let match;
    while ((match = mapping.exec(source)) !== null) {
      endpoints.push({
        method: match[1].toUpperCase(),
        path: normalizeBackendPath(classPrefix + (match[2] ?? '')),
        file: path.relative(root, file),
      });
    }
  }
  return endpoints;
}

function frontendCalls(root) {
  const files = walk(path.join(root, 'frontend/src'),
    (file) => file.endsWith('.js') || file.endsWith('.vue'));
  const calls = [];

  for (const file of files) {
    const lines = fs.readFileSync(file, 'utf8').split(/\r?\n/);
    lines.forEach((line, index) => {
      const url = line.match(/url\s*:\s*[`"'](\/[A-Za-z0-9_./-]*)/);
      const method = line.match(/method\s*:\s*["'](get|post|put|delete|patch)["']/i);
      if (url && method) {
        calls.push({
          method: method[1].toUpperCase(),
          prefix: url[1],
          file: path.relative(root, file),
          line: index + 1,
        });
      }
    });
  }
  return calls;
}

function matches(call, endpoint) {
  if (call.method !== endpoint.method) return false;
  if (endpoint.path === call.prefix) return true;
  if (endpoint.path.startsWith(call.prefix) && endpoint.path.slice(call.prefix.length).startsWith(':param')) return true;
  return false;
}

for (const candidate of candidates) {
  const root = path.join(verifyRoot, candidate);
  const endpoints = backendEndpoints(root);
  const calls = frontendCalls(root);
  const unmatched = calls.filter((call) => !endpoints.some((endpoint) => matches(call, endpoint)));

  console.log(`================ ${candidate} ================`);
  console.log(`backend_endpoints=${endpoints.length}`);
  console.log(`frontend_literal_calls=${calls.length}`);
  console.log(`unmatched_calls=${unmatched.length}`);
  for (const call of unmatched) {
    console.log(`${call.method} ${call.prefix} @ ${call.file}:${call.line}`);
  }
  console.log();
}
