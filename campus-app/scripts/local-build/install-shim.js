/**
 * 【仅本机/受限沙箱使用的构建适配脚本，不属于业务代码】
 *
 * 背景：本项目在一台「禁止创建管道子进程」的 Windows 沙箱里开发
 *  - npm install 的 postinstall（esbuild）会因 spawn EPERM 失败 → 需 `npm install --ignore-scripts`
 *  - esbuild JS API 内部 spawn 子进程走的是管道 → 任何 transform/build 都会 EPERM
 *  - vite 启动时优化 realpath 会 `exec('net use')` 探测网络盘 → 同步抛 EPERM，被内部 catch 吞掉，
 *    进而误判 sass 等预处理器「未安装」
 *
 * 本脚本在 node_modules 里做两处「本地补丁」，让 `npm run dev:h5` / `npm run build:h5` 能跑通：
 *  1) node_modules/esbuild/lib/main.js ← esbuild-shim-main.js（改为调用同目录的 esbuild.exe + 文件重定向，不建管道）
 *  2) node_modules/vite/dist/node/chunks/dep-*.js 中短路 `exec('net use', ...)` 网络盘探测
 *
 * 普通开发机（能正常 spawn 子进程）**不需要**执行本脚本，直接 npm install && npm run dev:h5 即可。
 * 撤销：node scripts/local-build/restore.js
 */
const fs = require('fs')
const path = require('path')

const root = path.join(__dirname, '..')
const shim = path.join(__dirname, 'esbuild-shim-main.js')

function patchEsbuild() {
  const libDir = path.join(root, 'node_modules', 'esbuild', 'lib')
  const main = path.join(libDir, 'main.js')
  const backup = path.join(libDir, 'main.real.js')
  if (!fs.existsSync(main)) {
    console.log('[skip] esbuild 未安装，请先 npm install --ignore-scripts')
    return
  }
  if (!fs.existsSync(backup)) {
    fs.copyFileSync(main, backup)
    console.log('[ok] 已备份 esbuild/lib/main.js → main.real.js')
  }
  fs.copyFileSync(shim, main)
  console.log('[ok] 已安装 esbuild shim')
}

function patchVite() {
  const chunkDir = path.join(root, 'node_modules', 'vite', 'dist', 'node', 'chunks')
  if (!fs.existsSync(chunkDir)) {
    console.log('[skip] vite 未安装')
    return
  }
  const files = fs.readdirSync(chunkDir).filter((f) => /^dep-.*\.js$/.test(f))
  let patched = 0
  files.forEach((file) => {
    const target = path.join(chunkDir, file)
    const backup = target + '.bak'
    let source = fs.existsSync(backup) ? fs.readFileSync(backup, 'utf8') : fs.readFileSync(target, 'utf8')
    const start = source.indexOf("    exec('net use', (error, stdout) => {")
    const endMark = '    });\n}\nfunction ensureWatchedFile('
    const end = source.indexOf(endMark)
    if (start < 0 || end < 0 || end < start) {
      // 该 chunk 不含网络盘探测逻辑，跳过
      return
    }
    if (!fs.existsSync(backup)) fs.copyFileSync(target, backup)
    const replacement =
      '    // [DSH-PATCH] 跳过网络盘探测（沙箱禁止管道子进程，exec 会抛 EPERM）\n' +
      '    safeRealpathSync = fs$l.realpathSync.native;\n}\nfunction ensureWatchedFile('
    source = source.slice(0, start) + replacement + source.slice(end + endMark.length)
    fs.writeFileSync(target, source)
    patched += 1
    console.log('[ok] 已跳过网络盘探测:', file)
  })
  if (!patched) console.log('[skip] 未找到需要打补丁的 vite chunk')
}

patchEsbuild()
patchVite()
console.log('\n完成。现在可以执行：npm run dev:h5 / npm run build:h5')
