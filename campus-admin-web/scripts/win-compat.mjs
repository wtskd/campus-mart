/**
 * Windows 环境兼容补丁（必须在 import vite 之前引入）
 *
 * 背景：Vite 5 在 Windows 上首次进行路径解析时，会通过 child_process.exec 执行
 * `net use` 来探测「网络映射盘符」，用于修正 realpath 结果。该子进程在部分受限
 * 环境（禁止创建子进程管道 / 无命令行权限）下会直接抛 EPERM，导致 Vite 启动或构建失败。
 *
 * 处理：让 fs.realpathSync.native 抛出一个 Vite 能识别的 EISDIR 错误，Vite 捕获后会
 * 回退到等价的 fs.realpathSync 实现，从而完全跳过 `net use` 子进程调用。
 * 本项目位于本地磁盘，回退后的路径语义一致，不影响功能。
 */
import fs from 'node:fs'

if (process.platform === 'win32') {
  try {
    fs.realpathSync.native = (p) => {
      const error = new Error(`EISDIR: illegal operation on a directory, realpath '${p}'`)
      error.code = 'EISDIR'
      throw error
    }
  } catch {
    // 忽略：补丁失败时仍走 Vite 默认逻辑
  }
}
