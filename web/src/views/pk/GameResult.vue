<template>
  <div class="game-result-overlay" @click="$emit('close')">
    <div class="game-result-modal" @click.stop>
      <div class="result-icon">
        <span v-if="winner === 'black'" class="winner-black">⚫</span>
        <span v-else-if="winner === 'white'" class="winner-white">⚪</span>
        <span v-else class="winner-draw">🤝</span>
      </div>
      
      <h2 class="result-title">
        <span v-if="winner === 'black'">黑方获胜！</span>
        <span v-else-if="winner === 'white'">白方获胜！</span>
        <span v-else>平局！</span>
      </h2>
      
      <p class="result-message">
        <span v-if="winner !== 'draw'">
          恭喜{{ winner === 'black' ? '黑方' : '白方' }}赢得本局比赛！
        </span>
        <span v-else>
          棋逢对手，不分胜负！
        </span>
      </p>
      
      <div class="result-actions">
        <button @click="$emit('restart')" class="action-btn primary">
          再来一局
        </button>
        <button @click="$emit('close')" class="action-btn secondary">
          关闭
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  winner: {
    type: String,
    required: true
  }
})

defineEmits(['close', 'restart'])
</script>

<style scoped>
.game-result-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.game-result-modal {
  background: white;
  padding: 40px;
  border-radius: 20px;
  text-align: center;
  max-width: 400px;
  width: 90%;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from {
    transform: translateY(30px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.result-icon {
  font-size: 4rem;
  margin-bottom: 20px;
}

.winner-black,
.winner-white,
.winner-draw {
  display: inline-block;
  animation: bounce 0.6s ease infinite alternate;
}

@keyframes bounce {
  from {
    transform: translateY(0);
  }
  to {
    transform: translateY(-10px);
  }
}

.result-title {
  margin: 0 0 15px 0;
  font-size: 2rem;
  color: #333;
  font-weight: bold;
}

.result-message {
  margin: 0 0 30px 0;
  color: #666;
  font-size: 1.1rem;
  line-height: 1.5;
}

.result-actions {
  display: flex;
  gap: 15px;
  justify-content: center;
}

.action-btn {
  padding: 12px 30px;
  font-size: 1rem;
  border: none;
  border-radius: 25px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-weight: bold;
  min-width: 120px;
}

.action-btn.primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.action-btn.primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
}

.action-btn.secondary {
  background: #f0f0f0;
  color: #333;
}

.action-btn.secondary:hover {
  background: #e0e0e0;
  transform: translateY(-2px);
}
</style>
