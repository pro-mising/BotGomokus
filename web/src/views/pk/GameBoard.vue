<template>
  <div class="game-board">
    <div class="board-container">
      <div 
        v-for="(row, rowIndex) in board" 
        :key="rowIndex" 
        class="board-row"
      >
        <div
          v-for="(cell, colIndex) in row"
          :key="colIndex"
          class="board-cell"
          :class="{ 
            'last-move': isLastMove(rowIndex, colIndex),
            'hover-black': !cell && currentPlayer === 'black' && !gameOver,
            'hover-white': !cell && currentPlayer === 'white' && !gameOver
          }"
          @click="handleCellClick(rowIndex, colIndex)"
        >
          <div v-if="cell" class="piece" :class="cell">
            <div v-if="isLastMove(rowIndex, colIndex)" class="last-move-marker"></div>
          </div>
          <div class="intersection"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useStore } from 'vuex'

const store = useStore()

const board = computed(() => store.getters['pk/board'])
const currentPlayer = computed(() => store.getters['pk/currentPlayer'])
const gameOver = computed(() => store.getters['pk/gameOver'])
const lastMove = computed(() => store.getters['pk/lastMove'])

const isLastMove = (row, col) => {
  return lastMove.value && lastMove.value.row === row && lastMove.value.col === col
}

const handleCellClick = (row, col) => {
  if (gameOver.value || board.value[row][col]) return
  store.dispatch('pk/makeMove', { row, col })
}
</script>

<style scoped>
.game-board {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
}

.board-container {
  background: #e8c170;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  border: 3px solid #8b6914;
}

.board-row {
  display: flex;
}

.board-cell {
  width: 40px;
  height: 40px;
  position: relative;
  cursor: pointer;
  display: flex;
  justify-content: center;
  align-items: center;
}

.board-cell:hover .intersection {
  background: rgba(0, 0, 0, 0.1);
}

.intersection {
  position: absolute;
  width: 100%;
  height: 100%;
}

.intersection::before,
.intersection::after {
  content: '';
  position: absolute;
  background: #5c4a1f;
}

.intersection::before {
  width: 100%;
  height: 2px;
  top: 50%;
  transform: translateY(-50%);
}

.intersection::after {
  height: 100%;
  width: 2px;
  left: 50%;
  transform: translateX(-50%);
}

.piece {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  position: relative;
  z-index: 2;
  transition: transform 0.2s ease;
}

.piece.black {
  background: radial-gradient(circle at 30% 30%, #555, #000);
  box-shadow: 2px 2px 4px rgba(0, 0, 0, 0.5);
}

.piece.white {
  background: radial-gradient(circle at 30% 30%, #fff, #ddd);
  box-shadow: 2px 2px 4px rgba(0, 0, 0, 0.3);
  border: 1px solid #ccc;
}

.last-move-marker {
  position: absolute;
  width: 8px;
  height: 8px;
  background: #ff4444;
  border-radius: 50%;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  box-shadow: 0 0 4px rgba(255, 68, 68, 0.8);
}

.last-move .piece {
  transform: scale(1.1);
}

.board-cell.hover-black:hover::before,
.board-cell.hover-white:hover::before {
  content: '';
  position: absolute;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  opacity: 0.5;
  z-index: 1;
}

.board-cell.hover-black:hover::before {
  background: radial-gradient(circle at 30% 30%, #555, #000);
}

.board-cell.hover-white:hover::before {
  background: radial-gradient(circle at 30% 30%, #fff, #ddd);
  border: 1px solid #ccc;
}
</style>
