// 本地验证专用补丁：还原 vite dist，并短路 optimizeSafeRealPathSync 中的 exec('net use')
// 原因：DSH 沙箱禁止带管道的子进程，exec 同步抛 EPERM，被 resolvePackageData 的 try/catch 吞掉，
//      导致 vite 误判 sass 等预处理器包「未安装」。此处仅改本机 node_modules（交付物不含）。
const fs = require('fs')
const path = require('path')
const dir = path.join(__dirname, '..', 'node_modules', 'vite', 'dist', 'node', 'chunks')
const target = path.join(dir, 'dep-whKeNLxG.js')
const bak = target + '.bak'
if (!fs.existsSync(bak)) {
  console.log('NO BAK, abort')
  process.exit(1)
}
let src = fs.readFileSync(bak, 'utf8')
const start = src.indexOf("    exec('net use', (error, stdout) => {")
const endMark = "    });\n}\nfunction ensureWatchedFile("
const end = src.indexOf(endMark)
if (start < 0 || end < 0 || end < start) {
  console.log('PATTERN NOT FOUND', start, end)
  process.exit(1)
}
const replacement = "    // [DSH-PATCH] 跳过网络盘探测（沙箱禁止管道子进程，exec 会抛 EPERM）\n    safeRealpathSync = fs$l.realpathSync.native;\n}\nfunction ensureWatchedFile("
src = src.slice(0, start) + replacement + src.slice(end + endMark.length)
fs.writeFileSync(target, src)
console.log('vite dist patched ok, size=' + src.length)
