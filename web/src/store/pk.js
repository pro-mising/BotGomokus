const checkWin = (board, row, col, player) => {
  const directions = [
    [[0, 1], [0, -1]],   // horizontal
    [[1, 0], [-1, 0]],   // vertical
    [[1, 1], [-1, -1]], // diagonal
    [[1, -1], [-1, 1]]  // anti-diagonal
  ]
  
  for (const [dir1, dir2] of directions) {
    let count = 1
    
    for (const [dr, dc] of [dir1, dir2]) {
      let r = row + dr
      let c = col + dc
      while (r >= 0 && r < board.length && c >= 0 && c < board.length && board[r][c] === player) {
        count++
        r += dr
        c += dc
      }
    }
    
    if (count >= 5) return true
  }
  return false
}

const checkDraw = (board) => {
  return board.every(row => row.every(cell => cell !== null))
}

export default {
  namespaced: true,
  state: {
    game: {
      board: [],
      currentPlayer: 'black',
      gameOver: false,
      winner: null,
      lastMove: null
    },
    boardSize: 15,
    showResult: false
  },
  mutations: {
    initGame(state) {
      state.game.board = Array(state.boardSize).fill(null).map(() => Array(state.boardSize).fill(null))
      state.game.currentPlayer = 'black'
      state.game.gameOver = false
      state.game.winner = null
      state.game.lastMove = null
      state.showResult = false
    },
    makeMove(state, { row, col }) {
      if (state.game.gameOver || state.game.board[row][col]) return
      
      state.game.board[row][col] = state.game.currentPlayer
      state.game.lastMove = { row, col }
      
      if (checkWin(state.game.board, row, col, state.game.currentPlayer)) {
        state.game.gameOver = true
        state.game.winner = state.game.currentPlayer
        state.showResult = true
      } else if (checkDraw(state.game.board)) {
        state.game.gameOver = true
        state.game.winner = 'draw'
        state.showResult = true
      } else {
        state.game.currentPlayer = state.game.currentPlayer === 'black' ? 'white' : 'black'
      }
    },
    setShowResult(state, value) {
      state.showResult = value
    }
  },
  actions: {
    initGame({ commit }) {
      commit('initGame')
    },
    makeMove({ commit }, move) {
      commit('makeMove', move)
    },
    closeResult({ commit }) {
      commit('setShowResult', false)
    }
  },
  getters: {
    board: state => state.game.board,
    currentPlayer: state => state.game.currentPlayer,
    gameOver: state => state.game.gameOver,
    winner: state => state.game.winner,
    lastMove: state => state.game.lastMove,
    showResult: state => state.showResult
  }
}
