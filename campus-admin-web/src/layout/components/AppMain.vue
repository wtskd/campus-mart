<template>
  <section class="app-main">
    <router-view v-slot="{ Component, route: currentRoute }">
      <transition name="fade-transform" mode="out-in">
        <component :is="Component" :key="currentRoute.path" />
      </transition>
    </router-view>
  </section>
</template>

<style scoped>
.app-main {
  min-height: calc(100vh - 88px);
  padding: 14px;
  overflow-x: hidden;
}

.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all 0.2s;
}

.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(-8px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(8px);
}
</style>
