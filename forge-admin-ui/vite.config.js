import path from 'node:path'
import Vue from '@vitejs/plugin-vue'
import VueJsx from '@vitejs/plugin-vue-jsx'
import Unocss from 'unocss/vite'
import AutoImport from 'unplugin-auto-import/vite'
import { NaiveUiResolver } from 'unplugin-vue-components/resolvers'
import Components from 'unplugin-vue-components/vite'
import VueRouter from 'unplugin-vue-router/vite'
import { defineConfig, loadEnv } from 'vite'
import removeNoMatch from 'vite-plugin-router-warn'
import VueDevTools from 'vite-plugin-vue-devtools'
import { pluginIcons, pluginPagePathes } from './build/plugin-isme'

/**
 * 开发代理目标：须指向 Spring Boot 根（与 server.servlet.context-path=/ 一致）。
 * 若把生产站点地址 http://host/public/ims/ 原样填进 target，http-proxy 会请求 /public/ims/auth/...，
 * 而后端只有 /auth/...，会 404（全局异常里「请求的资源不存在」）。
 */
function resolveHttpProxyTarget(raw) {
  if (!raw)
    return raw
  try {
    const normalized = /:\/\//.test(raw) ? raw : `http://${raw}`
    const u = new URL(normalized.endsWith('/') ? normalized : `${normalized}/`)
    const pathOnly = (u.pathname || '/').replace(/\/$/, '')
    /** 已知「前端挂载前缀」，不是 JVM context-path（项目后端均为 /），开发代理应直连接口根 */
    const stripPrefixes = ['/public/ims']
    for (const prefix of stripPrefixes) {
      if (pathOnly === prefix || pathOnly.startsWith(`${prefix}/`)) {
        const origin = `${u.protocol}//${u.host}${u.port ? `:${u.port}` : ''}/`
        console.warn(
          `[vite] VITE_HTTP_PROXY_TARGET 含有站点前缀「${pathOnly}」，已改为「${origin}」再转发。\n`
          + '若你的 JVM 挂在 Nginx location 后才可访问（非直连），请改用内部端口或由运维提供「仅后端」的根地址。\n',
        )
        return origin
      }
    }
    return raw
  }
  catch {
    return raw
  }
}

export default defineConfig(({ mode }) => {
  const viteEnv = loadEnv(mode, process.cwd())
  const { VITE_HTTP_PORT, VITE_REQUEST_PREFIX, VITE_PUBLIC_PATH, VITE_HTTP_PROXY_TARGET, VITE_FLOW_PROXY_TARGET } = viteEnv
  const mainProxyTarget = resolveHttpProxyTarget(VITE_HTTP_PROXY_TARGET)

  return {
    base: VITE_PUBLIC_PATH || '/',
    plugins: [
      // unplugin-vue-router 必须在 Vue 之前
      VueRouter({
        routesFolder: [
          {
            src: 'src/views',
            path: '',
            // 排除不需要生成路由的文件
            exclude: ['**/components/**', '**/api/**'],
          },
        ],
        dts: false,
      }),
      Vue(),
      VueJsx(),
      VueDevTools(),
      Unocss(),
      AutoImport({
        imports: ['vue', 'vue-router'],
        dts: false,
      }),
      Components({
        resolvers: [NaiveUiResolver()],
        dts: false,
      }),
      // 自定义插件，用于生成页面文件的path，并添加到虚拟模块
      pluginPagePathes(),
      // 自定义插件，用于生成自定义icon，并添加到虚拟模块
      pluginIcons(),
      // 移除非必要的vue-router动态路由警告: No match found for location with path
      removeNoMatch(),
    ],
    resolve: {
      alias: {
        '@': path.resolve(process.cwd(), 'src'),
        '~': path.resolve(process.cwd()),
      },
    },
    define: {
      // 为某些依赖（例如 browser-crypto）提供全局对象
      global: 'window',
    },
    css: {
      preprocessorOptions: {
        scss: {
          additionalData: `@use "@/styles/variables.scss";`,
        },
      },
    },
    server: {
      port: VITE_HTTP_PORT,
      host: '0.0.0.0',
      open: false,
      proxy: {
        // 流程服务代理 - 必须在主代理之前，匹配更具体的路径
        [`${VITE_REQUEST_PREFIX}/api/flow`]: {
          target: VITE_FLOW_PROXY_TARGET || 'http://localhost:8081',
          changeOrigin: true,
          secure: false,
          rewrite: path => path.replace(/^\/[^/]+/, ''),
        },
        // 主代理 - 匹配所有其他请求
        [VITE_REQUEST_PREFIX]: {
          secure: false,
          target: mainProxyTarget,
          changeOrigin: true,
          rewrite: path => path.replace(new RegExp(`^${VITE_REQUEST_PREFIX}`), ''),
          configure: (proxy, options) => {
            // 配置此项可在响应头中看到请求的真实地址
            proxy.on('proxyRes', (proxyRes, req) => {
              proxyRes.headers['x-real-url'] = new URL(req.url || '', options.target)?.href || ''
            })
          },
        },
        // WebSocket 代理到后端同一服务
        '/ws': {
          target: mainProxyTarget,
          changeOrigin: true,
          ws: true,
          secure: false,
        },
      },
    },
    build: {
      chunkSizeWarningLimit: 1024, // chunk 大小警告的限制（单位kb）
    },
  }
})
