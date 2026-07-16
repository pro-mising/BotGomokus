<template>
  <div class="pk-container">
    <div class="game-header">
      <h1>五子棋对战</h1>
      <div class="turn-indicator">
        <span :class="{ active: currentPlayer === 'black' }" class="player-indicator black">
          <span class="piece black-piece"></span>
          黑方
        </span>
        <span class="vs">VS</span>
        <span :class="{ active: currentPlayer === 'white' }" class="player-indicator white">
          <span class="piece white-piece"></span>
          白方
        </span>
      </div>
      <button @click="restartGame" class="restart-btn">重新开始</button>
    </div>
    
    <div class="game-board-wrapper">
      <GameBoard />
    </div>
    
    <GameResult v-if="showResult" :winner="winner" @close="closeResult" @restart="restartGame" />
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useStore } from 'vuex'
import GameBoard from './GameBoard.vue'
import GameResult from './GameResult.vue'

const store = useStore()

const currentPlayer = computed(() => store.getters['pk/currentPlayer'])
const showResult = computed(() => store.getters['pk/showResult'])
const winner = computed(() => store.getters['pk/winner'])

onMounted(() => {
  store.dispatch('pk/initGame')
})

const restartGame = () => {
  store.dispatch('pk/initGame')
}

const closeResult = () => {
  store.dispatch('pk/closeResult')
}
</script>

<style scoped>
.pk-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.game-header {
  text-align: center;
  margin-bottom: 20px;
  color: white;
}

.game-header h1 {
  margin: 0 0 20px 0;
  font-size: 2.5rem;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.3);
}

.turn-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
  margin-bottom: 20px;
  font-size: 1.2rem;
}

.player-indicator {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 20px;
  border-radius: 25px;
  background: rgba(255, 255, 255, 0.2);
  transition: all 0.3s ease;
  opacity: 0.6;
}

.player-indicator.active {
  opacity: 1;
  background: rgba(255, 255, 255, 0.4);
  transform: scale(1.1);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
}

.piece {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: inline-block;
}

.black-piece {
  background: radial-gradient(circle at 30% 30%, #555, #000);
  box-shadow: 2px 2px 4px rgba(0, 0, 0, 0.5);
}

.white-piece {
  background: radial-gradient(circle at 30% 30%, #fff, #ddd);
  box-shadow: 2px 2px 4px rgba(0, 0, 0, 0.3);
  border: 1px solid #ccc;
}

.vs {
  font-weight: bold;
  font-size: 1rem;
  color: rgba(255, 255, 255, 0.8);
}

.restart-btn {
  padding: 12px 30px;
  font-size: 1rem;
  background: rgba(255, 255, 255, 0.9);
  color: #667eea;
  border: none;
  border-radius: 25px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-weight: bold;
}

.restart-btn:hover {
  background: white;
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
}

.game-board-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
}
</style>
