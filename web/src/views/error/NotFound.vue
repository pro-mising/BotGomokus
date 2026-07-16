<template>
  <div class="not-found">
    <h1>404</h1>
    <p>页面未找到</p>
    <router-link to="/pk" class="back-link">返回游戏</router-link>
  </div>
</template>

<script setup>
</script>

<style scoped>
.not-found {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.not-found h1 {
  font-size: 6rem;
  margin: 0;
  text-shadow: 4px 4px 8px rgba(0, 0, 0, 0.3);
}

.not-found p {
  font-size: 1.5rem;
  margin: 20px 0;
}

.back-link {
  padding: 12px 30px;
  background: rgba(255, 255, 255, 0.9);
  color: #667eea;
  text-decoration: none;
  border-radius: 25px;
  font-weight: bold;
  transition: all 0.3s ease;
}

.back-link:hover {
  background: white;
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
}
</style>
