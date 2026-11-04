'use strict'
/**
 * esbuild 兼容层（沙箱验证专用，不属于项目交付物）
 * 原因：当前 Windows 沙箱禁止创建匿名管道，esbuild/npm 的 stdio: 'pipe' 子进程会 EPERM。
 * 方案：改用「文件描述符重定向」的方式调用官方 esbuild.exe（不创建管道）：
 *   transform()  -> 写临时输入文件，esbuild.exe --loader=xx < in > out
 *   build()      -> 调 esbuild CLI 打包，读回 outfile 与 metafile
 */
const cp = require('child_process')
const fs = require('fs')
const path = require('path')

const EXE = path.join(__dirname, '..', '..', '@esbuild', 'win32-x64', 'esbuild.exe')

let counter = 0
function tmpRoot() {
  const base = process.env.ESBUILD_SHIM_TMP || path.join(process.cwd(), 'node_modules', '.cache', 'esbuild-shim')
  try {
    fs.mkdirSync(base, { recursive: true })
  } catch (e) {
    /* ignore */
  }
  return base
}
function newTmp(ext) {
  counter += 1
  return path.join(tmpRoot(), 'esb-' + process.pid + '-' + counter + ext)
}

function runCli(args, input, cwd) {
  const inPath = newTmp('.in')
  const outPath = newTmp('.out')
  const errPath = newTmp('.err')
  fs.writeFileSync(inPath, input == null ? '' : input)
  const fds = [fs.openSync(inPath, 'r'), fs.openSync(outPath, 'w'), fs.openSync(errPath, 'w')]
  let r
  try {
    r = cp.spawnSync(EXE, args, { stdio: fds, cwd: cwd || process.cwd(), windowsHide: true })
  } finally {
    for (const fd of fds) {
      try {
        fs.closeSync(fd)
      } catch (e) {
        /* ignore */
      }
    }
  }
  const stdout = fs.readFileSync(outPath, 'utf8')
  const stderr = fs.readFileSync(errPath, 'utf8')
  for (const p of [inPath, outPath, errPath]) {
    try {
      fs.unlinkSync(p)
    } catch (e) {
      /* ignore */
    }
  }
  return { status: r.status, stdout, stderr, error: r.error }
}

function makeError(text) {
  const lines = String(text || '').trim().split(/\r?\n/).filter(Boolean)
  const err = new Error(lines[0] || 'esbuild failed')
  err.errors = lines.map((l) => ({ text: l, location: null, notes: [] }))
  err.warnings = []
  return err
}

function pushTransformArgs(args, o) {
  if (o.target) args.push('--target=' + (Array.isArray(o.target) ? o.target.join(',') : o.target))
  if (o.charset) args.push('--charset=' + o.charset)
  if (o.format) args.push('--format=' + o.format)
  if (o.define) {
    for (const k of Object.keys(o.define)) args.push('--define:' + k + '=' + o.define[k])
  }
  if (o.minify) args.push('--minify')
  if (o.minifyWhitespace) args.push('--minify-whitespace')
  if (o.minifyIdentifiers) args.push('--minify-identifiers')
  if (o.minifySyntax) args.push('--minify-syntax')
  if (o.treeShaking) args.push('--tree-shaking=' + o.treeShaking)
  if (o.legalComments) args.push('--legal-comments=' + o.legalComments)
  if (o.jsx) args.push('--jsx=' + o.jsx)
  if (o.jsxFactory) args.push('--jsx-factory=' + o.jsxFactory)
  if (o.jsxFragment) args.push('--jsx-fragment=' + o.jsxFragment)
  if (o.jsxImportSource) args.push('--jsx-import-source=' + o.jsxImportSource)
  if (o.banner) args.push('--banner:js=' + o.banner)
  if (o.footer) args.push('--footer:js=' + o.footer)
  if (o.pure) (Array.isArray(o.pure) ? o.pure : [o.pure]).forEach((p) => args.push('--pure:' + p))
  if (o.drop) (Array.isArray(o.drop) ? o.drop : [o.drop]).forEach((d) => args.push('--drop:' + d))
  if (o.keepNames) args.push('--keep-names')
  if (o.tsconfigRaw) {
    // transform 模式不支持 --tsconfig，统一改传内联 JSON
    const raw = typeof o.tsconfigRaw === 'string' ? o.tsconfigRaw : JSON.stringify(o.tsconfigRaw)
    args.push('--tsconfig-raw=' + raw)
  }
}

function transformImpl(code, options) {
  const o = options || {}
  const args = []
  if (typeof o.loader === 'string') args.push('--loader=' + o.loader)
  pushTransformArgs(args, o)
  const res = runCli(args, String(code))
  if (res.error) throw makeError(res.error.message)
  if (res.status !== 0) throw makeError(res.stderr)
  let map = ''
  if (o.sourcemap) {
    map = JSON.stringify({
      version: 3,
      sources: [o.sourcefile || 'stdin'],
      sourcesContent: [String(code)],
      names: [],
      mappings: ''
    })
  }
  return { code: res.stdout, map, warnings: [], legalComments: [] }
}

function normalizeEntries(entryPoints) {
  const list = Array.isArray(entryPoints) ? entryPoints : [entryPoints]
  return list.map((e) => (typeof e === 'string' ? { in: e, out: null } : e))
}

function buildImpl(options) {
  const o = options || {}
  const entries = normalizeEntries(o.entryPoints || [])
  if (entries.length !== 1) {
    throw makeError('esbuild-shim: only a single entry point is supported')
  }
  const outfile = o.outfile ? path.resolve(o.absWorkingDir || process.cwd(), o.outfile) : newTmp('.out.js')
  try {
    fs.mkdirSync(path.dirname(outfile), { recursive: true })
  } catch (e) {
    /* ignore */
  }
  const args = [entries[0].in, '--bundle', '--outfile=' + outfile]
  if (o.format) args.push('--format=' + o.format)
  if (o.platform) args.push('--platform=' + o.platform)
  if (o.mainFields) args.push('--main-fields=' + o.mainFields.join(','))
  if (o.conditions) args.push('--conditions=' + o.conditions.join(','))
  if (o.sourcemap) args.push('--sourcemap=' + (typeof o.sourcemap === 'string' ? o.sourcemap : 'inline'))
  if (o.packages) args.push('--packages=' + o.packages)
  else if (!o.external || !o.external.length) args.push('--packages=external')
  if (o.external) o.external.forEach((e) => args.push('--external:' + e))
  if (o.keepNames) args.push('--keep-names')
  for (const k of Object.keys(o.define || {})) args.push('--define:' + k + '=' + o.define[k])
  for (const k of Object.keys(o.loader || {})) args.push('--loader:' + k + '=' + o.loader[k])
  let metaPath = ''
  if (o.metafile) {
    metaPath = typeof o.metafile === 'string' ? o.metafile : newTmp('.meta.json')
    args.push('--metafile=' + metaPath)
  }
  args.push('--log-level=warning')
  const res = runCli(args, '', o.absWorkingDir)
  if (res.error) throw makeError(res.error.message)
  if (res.status !== 0) throw makeError(res.stderr)
  const text = fs.existsSync(outfile) ? fs.readFileSync(outfile, 'utf8') : ''
  let metafile
  if (metaPath) {
    try {
      metafile = JSON.parse(fs.readFileSync(metaPath, 'utf8'))
    } catch (e) {
      metafile = { inputs: {}, outputs: {} }
    }
  }
  if (o.write === false) {
    try {
      fs.unlinkSync(outfile)
    } catch (e) {
      /* ignore */
    }
  }
  return {
    outputFiles: [{ path: outfile, text, contents: Buffer.from(text) }],
    metafile,
    warnings: [],
    mtimeMs: Date.now()
  }
}

function formatMessages(messages, opts) {
  const kind = (opts && opts.kind) || 'error'
  return (messages || []).map((m) => {
    if (typeof m === 'string') return m
    let loc = ''
    if (m.location && m.location.file) loc = m.location.file + ':' + (m.location.line || 0) + ':' + (m.location.column || 0) + ': '
    return loc + (m.text || kind)
  })
}

module.exports.version = '0.20.2'
module.exports.platform = process.platform
module.exports.transform = async function transform(code, options) {
  return transformImpl(code, options)
}
module.exports.transformSync = function transformSync(code, options) {
  return transformImpl(code, options)
}
module.exports.build = async function build(options) {
  return buildImpl(options)
}
module.exports.buildSync = function buildSync(options) {
  return buildImpl(options)
}
module.exports.formatMessages = async function formatMessagesAsync(messages, opts) {
  return formatMessages(messages, opts)
}
module.exports.formatMessagesSync = formatMessages
module.exports.analyzeMetafile = async function analyzeMetafile(metafile) {
  return JSON.stringify(metafile, null, 2)
}
module.exports.analyzeMetafileSync = function analyzeMetafileSync(metafile) {
  return JSON.stringify(metafile, null, 2)
}
module.exports.context = async function context() {
  throw makeError('esbuild-shim: context() is not supported')
}
module.exports.stop = function stop() {
  return Promise.resolve()
}
module.exports.initialize = async function initialize() {}
module.exports.default = module.exports
