<script lang="ts">
    import { onMount, onDestroy } from 'svelte';
    import { createSocketStore } from '$lib/services/api';

    interface GameInput {
        messageType: string;
        theme: string;
        subjects: string[];
        ommittedSubject: string;
        hint: string[];
    }

    interface PlayerNote {
        playerName: string;
        note: string;
    }

    interface PlayerState {
        messageType: string;
        isSpy: Boolean;
        theme: string;
        subject: string;
        subjectList: List<string>;
        hint: string;
        playerNotes: PlayerNote[];
    }

    let socket: WebSocket;
    let players: string[] = [];
    let playerName = '';
    let isConnected = false;
    let gameState: PlayerState | null = null;

    function updatePlayerNote(playerName: string, note: string) {
        if (gameState) {
            const noteIndex = gameState.playerNotes.findIndex(n => n.playerName === playerName);
            if (noteIndex >= 0) {
                gameState.playerNotes[noteIndex].note = note;
            } else {
                gameState.playerNotes.push({ playerName, note });
            }
            gameState = { ...gameState }; // Trigger reactivity
            saveGameStateToLocalStorage();
        }
    }

    function getPlayerNote(playerName: string): string {
        if (!gameState?.playerNotes) return '';
        const note = gameState.playerNotes.find(n => n.playerName === playerName);
        return note?.note || '';
    }

    function connectWebSocket() {
        if (!playerName.trim()) return;

        savePlayerNameToLocalStorage();

        socket = createSocketStore('socket/play');

        socket.onopen = () => {
            isConnected = true;
            socket.send(playerName);
            loadGameStateFromLocalStorage();
        };

        socket.onmessage = (event) => {
            console.log(event)
            const data = JSON.parse(event.data);
            console.log(data)

            if (data.messageType === "PlayerState") {
                // Initialize playerNotes if it doesn't exist
                if (!data.playerNotes) {
                    data.playerNotes = [];
                }
                gameState = data;
                saveGameStateToLocalStorage();
            } else {
                players = data.connectedPlayers;
            }
        };

        socket.onclose = () => {
            isConnected = false;
        };
    }

    function loadGameStateFromLocalStorage() {
        const storedGameState = localStorage.getItem('gameState');
        if (storedGameState) {
            gameState = JSON.parse(storedGameState);
            if (!gameState.playerNotes) {
                gameState.playerNotes = [];
            }
        }
    }

    function saveGameStateToLocalStorage() {
        if (gameState) {
            localStorage.setItem('gameState', JSON.stringify(gameState));
        }
    }

    function loadPlayerNameFromLocalStorage() {
        const storedPlayerName = localStorage.getItem('playerName');
        if (storedPlayerName) {
            playerName = storedPlayerName;
        }
    }

    function savePlayerNameToLocalStorage() {
        localStorage.setItem('playerName', playerName);
    }

    onMount(() => {
        loadPlayerNameFromLocalStorage();
        loadGameStateFromLocalStorage();
        connectWebSocket();
    });

    onDestroy(() => {
        if (socket) {
            socket.close();
        }
    });
</script>

<div class="container">
    {#if !isConnected}
        <div class="join-form">
            <input
                type="text"
                bind:value={playerName}
                placeholder="Enter your name"
                class="name-input"
            />
            <button
                on:click={connectWebSocket}
                class="join-button"
                disabled={!playerName.trim()}
            >
                Join Game
            </button>
        </div>
    {:else}
        <div class="game-setup">
            {#if gameState}
                <div class="game-state">
                    {#if gameState.isSpy}
                        <div class="spy-view">
                            <div class="role-badge spy">You are the Spy!</div>
                            <p class="spy-instruction">Try to guess the theme.</p>
                            <div class="hint-box">
                                <span class="hint-label">Hint:</span>
                                <span class="hint-text">{gameState.hint}</span>
                            </div>
                            <div class="player-notes">
                                <h3>Player Notes</h3>
                                <div class="notes-list">
                                    {#each gameState.allPlayers as player}
                                        <div class="note-item">
                                            <div class="note-header">
                                                <span class="player-name">{player}</span>
                                                {#if player === playerName}
                                                    <span class="you-badge">You</span>
                                                {/if}
                                            </div>
                                            <input
                                                type="text"
                                                class="note-input"
                                                value={getPlayerNote(player)}
                                                on:input={(e) => updatePlayerNote(player, e.target.value)}
                                                placeholder="Notes..."
                                            />
                                        </div>
                                    {/each}
                                </div>
                            </div>
                        </div>
                    {:else}
                        <div class="player-view">
                            <div class="role-badge regular">Regular Player</div>
                            <div class="theme-box">
                                <span class="theme-label">Theme:</span>
                                <span class="theme-text">{gameState.theme}</span>
                            </div>
                            <div class="subject-box">
                                <span class="subject-label">Your Subject:</span>
                                <span class="subject-text">{gameState.subject}</span>
                            </div>
                            {#if gameState.subjectList.length > 0}
                            <div class="subjects-list">
                                <span class="subjects-label">Other Subjects:</span>
                                <div class="subjects-grid">
                                    {#each gameState.subjectList as subject}
                                        <span class="subject-item">{subject}</span>
                                    {/each}
                                </div>
                            </div>
                            {/if}
                            <div class="player-notes">
                                <h3>Player Notes</h3>
                                <div class="notes-list">
                                    {#each gameState.allPlayers as player}
                                        <div class="note-item">
                                            <div class="note-header">
                                                <span class="player-name">{player}</span>
                                                {#if player === playerName}
                                                    <span class="you-badge">You</span>
                                                {/if}
                                            </div>
                                            <input
                                                type="text"
                                                class="note-input"
                                                value={getPlayerNote(player)}
                                                on:input={(e) => updatePlayerNote(player, e.target.value)}
                                                placeholder="Notes..."
                                            />
                                        </div>
                                    {/each}
                                </div>
                            </div>
                        </div>
                    {/if}
                </div>
            {:else}
                <div class="players-list">
                    <h2>Connected Players</h2>
                    <div class="players">
                        {#each players as player}
                            <div class="player-card">
                                <span class="player-name">{player}</span>
                                {#if player === playerName}
                                    <span class="you-badge">You</span>
                                {/if}
                            </div>
                        {/each}
                    </div>
                </div>
            {/if}
        </div>
    {/if}
</div>

<style>

    .player-notes {
        margin-top: 1.5rem;
    }

    .player-notes h3 {
        color: white;
        margin-bottom: 0.75rem;
        font-size: 1.1rem;
    }

    .notes-list {
        display: flex;
        flex-direction: column;
        gap: 0.5rem;
    }

    .note-item {
        background: rgba(44, 82, 130, 0.3);
        padding: 0.5rem;
        border-radius: 0.375rem;
    }

    .note-header {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        margin-bottom: 0.25rem;
    }

    .note-input {
        width: 100%;
        background: rgba(255, 255, 255, 0.1);
        border: 1px solid rgba(255, 255, 255, 0.2);
        border-radius: 0.25rem;
        color: white;
        padding: 0.25rem 0.5rem;
        height: 2rem;
        font-size: 0.9rem;
    }

    .note-input::placeholder {
        color: rgba(255, 255, 255, 0.5);
    }
.container {
        max-width: 800px;
        margin: 2rem auto;
        padding: 1rem;
    }

    .join-form {
        display: flex;
        flex-direction: column;
        gap: 1rem;
        align-items: center;
    }

    .name-input {
        padding: 0.5rem 1rem;
        border: 2px solid #2c5282;
        border-radius: 0.5rem;
        background: rgba(255, 255, 255, 0.1);
        color: white;
        font-size: 1.1rem;
        width: 100%;
        max-width: 300px;
    }

    .name-input::placeholder {
        color: rgba(255, 255, 255, 0.5);
    }

    button {
        padding: 0.5rem 2rem;
        background: #2c5282;
        color: white;
        border: none;
        border-radius: 0.5rem;
        cursor: pointer;
        font-size: 1.1rem;
        transition: background 0.2s;
    }

    button:hover:not(:disabled) {
        background: #3182ce;
    }

    button:disabled {
        opacity: 0.5;
        cursor: not-allowed;
    }

    .players-list {
        background: rgba(255, 255, 255, 0.1);
        border-radius: 1rem;
        padding: 1.5rem;
        margin-bottom: 2rem;
    }

    h2 {
        color: white;
        text-align: center;
        margin: 0 0 1.5rem 0;
    }

    .players {
        display: grid;
        gap: 1rem;
    }

    .player-card {
        background: rgba(44, 82, 130, 0.3);
        padding: 0.75rem 1rem;
        border-radius: 0.5rem;
        color: white;
        display: flex;
        justify-content: space-between;
        align-items: center;
    }

    .player-name {
        font-size: 1.1rem;
    }

    .you-badge {
        background: #2c5282;
        padding: 0.2rem 0.5rem;
        border-radius: 0.25rem;
        font-size: 0.8rem;
    }

    input {
        width: 100%;
        padding: 0.5rem;
        background: rgba(255, 255, 255, 0.1);
        border: 2px solid #2c5282;
        border-radius: 0.5rem;
        color: white;
        font-size: 1rem;
    }

    input::placeholder {
        color: rgba(255, 255, 255, 0.5);
    }

    .game-state {
        background: rgba(255, 255, 255, 0.1);
        border-radius: 1rem;
        padding: 1.5rem;
        margin-top: 2rem;
    }

    .role-badge {
        display: inline-block;
        padding: 0.5rem 1rem;
        border-radius: 0.5rem;
        font-weight: bold;
        margin-bottom: 1rem;
    }

    .role-badge.spy {
        background: #e53e3e;
        color: white;
    }

    .role-badge.regular {
        background: #2c5282;
        color: white;
    }

    .spy-instruction {
        font-size: 1.2rem;
        color: white;
        margin-bottom: 1rem;
    }

    .hint-box, .theme-box, .subject-box {
        background: rgba(44, 82, 130, 0.3);
        padding: 1rem;
        border-radius: 0.5rem;
        margin-bottom: 1rem;
    }

    .hint-label, .theme-label, .subject-label, .subjects-label {
        display: block;
        color: rgba(255, 255, 255, 0.7);
        font-size: 0.9rem;
        margin-bottom: 0.5rem;
    }

    .hint-text, .theme-text, .subject-text {
        color: white;
        font-size: 1.2rem;
        font-weight: bold;
    }

    .subjects-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
        gap: 0.5rem;
        margin-top: 0.5rem;
    }

    .subject-item {
        background: rgba(44, 82, 130, 0.3);
        padding: 0.5rem;
        border-radius: 0.25rem;
        color: white;
        text-align: center;
    }

    .subjects-list {
        margin-top: 1rem;
    }
</style>
