<script lang="ts">
    import { onMount, onDestroy } from 'svelte';
    import { createSocketStore } from '$lib/services/api';

    interface GameInput {
        messageType: string;
        theme: string;
        subjects: string[];
        ommittedSubject: string;
        hint: string;
    }

    let socket: WebSocket | null = null;
    let players: string[] = [];
    let isGameStarted = false; // Track whether the game has started

    // Form data
    let theme = '';
    let subjectsInput = ''; // Will be split  into array
    let ommittedSubject = '';
    let hint = '';

    $: subjects = subjectsInput.split(',').map(s => s.trim()).filter(s => s);
    $: isStartGameEnabled = players.length < subjects.length;

     onMount(() => {
        socket = createSocketStore('socket/admin');
        socket.onmessage = (event) => {
            players = JSON.parse(event.data).connectedPlayers;
        };
    });

    function handleSubmit() {
        const gameInput: GameInput = {
            messageType: "GameInput",
            theme,
            subjects: subjectsInput.split(',').map(s => s.trim()).filter(s => s),
            ommittedSubject,
            hint
        };

        socket.send(JSON.stringify(gameInput));
        isGameStarted = true;
    }


    onDestroy(() => {
        if (socket) {
            socket.close();
        }
    });
</script>

<div class="container">
        {#if isGameStarted}
            <h2 class="enjoy-message">Enjoy the game!</h2>
        {:else}
        <div class="game-setup">
            <form class="setup-form" on:submit|preventDefault={handleSubmit}>
                <h2>Game Setup</h2>

                <div class="form-group">
                    <label for="theme">Theme</label>
                    <input
                        type="text"
                        id="theme"
                        bind:value={theme}
                        placeholder="Example: Days of the week"
                        required
                    />
                </div>

                <div class="form-group">
                    <label for="subjects">Subjects (comma-separated)</label>
                    <textarea
                        id="subjects"
                        bind:value={subjectsInput}
                        placeholder="Monday, Thursday, Saturday"
                        required
                    ></textarea>
                </div>

                <div class="form-group">
                    <label for="ommitted">The Spy's Subject</label>
                    <input
                        type="text"
                        id="ommitted"
                        bind:value={ommittedSubject}
                        placeholder="Sunday"
                        required
                    />
                </div>

                <div class="form-group">
                    <label for="hints">Hint</label>
                    <input
                        type="text"
                        id="hints"
                        bind:value={hint}
                        placeholder="Answer with something warm"
                        required
                    />
                </div>

                <button type="submit" class="submit-button"     disabled={!isStartGameEnabled}>Start Game</button>
            </form>
            <div class="players-list">
                <h2>Connected Players: {players.length}</h2>
                <div class="players">
                    {#each players as player}
                        <div class="player-card">
                            <span class="player-name">{player}</span>
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

    .setup-form {
        background: rgba(255, 255, 255, 0.1);
        border-radius: 1rem;
        padding: 1.5rem;
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

    .form-group {
        margin-bottom: 1.5rem;
    }

    label {
        display: block;
        color: white;
        margin-bottom: 0.5rem;
    }

    input, textarea {
        width: 100%;
        padding: 0.5rem;
        background: rgba(255, 255, 255, 0.1);
        border: 2px solid #2c5282;
        border-radius: 0.5rem;
        color: white;
        font-size: 1rem;
    }

    textarea {
        min-height: 100px;
        resize: vertical;
    }

    input::placeholder, textarea::placeholder {
        color: rgba(255, 255, 255, 0.5);
    }

    .submit-button {
        width: 100%;
        padding: 0.75rem;
        font-size: 1.2rem;
        margin-top: 1rem;
    }
</style>