<script lang="ts">
    import { onMount, onDestroy } from 'svelte';

    interface GameInput {
        messageType: string;
        theme: string;
        subjects: string[];
        ommittedSubject: string;
        hint: string[];
    }

    let socket: WebSocket;
    let players: string[] = [];
    let playerName = '';
    let isConnected = false;

    function connectWebSocket() {
        if (!playerName.trim()) return;

        socket = new WebSocket('ws://localhost:8089/play');

        socket.onopen = () => {
            isConnected = true;
            socket.send(playerName);
        };

        socket.onmessage = (event) => {
            players = JSON.parse(event.data).connectedPlayers;
        };

        socket.onclose = () => {
            isConnected = false;
        };
    }

    function handleSubmit() {
        const gameInput: GameInput = {
            messageType: "GameInput",
            theme,
            subjects: subjectsInput.split(',').map(s => s.trim()).filter(s => s),
            ommittedSubject,
            hint: hintsInput.split(',').map(s => s.trim()).filter(s => s)
        };

        socket.send(JSON.stringify(gameInput));
    }

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
        </div>
    {/if}
</div>

<style>
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

</style>