<script lang="ts">
    import { onMount, onDestroy } from 'svelte';
    import { createSocketStore } from '$lib/services/api';

    interface GameInput {
        messageType: string;
        theme: string;
        subjects: string[];
        numSpies: number; // New field to specify number of spies
        spySubjects: string[]; // New field to store subjects for spies
        spyHints: string[]; // New field to store hints for spies
        showSubjectsToNonSpies: boolean;
    }

    let socket: WebSocket | null = null;
    let players: string[] = [];
    let isGameStarted = false;

    let theme = '';
    let subjectsInput = '';
    let numSpies = 1; // Default number of spies
    let spySubjects = ['']; // Array to store subjects for each spy (initialize with empty string for default spy)
    let spyHints = ['']; // Array to store hints for each spy (initialize with empty string for default spy)
    let showSubjectsToNonSpies = false;

    $: subjects = subjectsInput.split(',').map(s => s.trim()).filter(s => s);
    $: isStartGameEnabled = numSpies + subjects.length >= players.length;

    $: {
        if (!isGameStarted && socket) {
            localStorage.setItem('gameSetup', JSON.stringify({
                theme,
                subjectsInput,
                numSpies,
                spySubjects,
                spyHints,
                showSubjectsToNonSpies
            }));
        }
    }

    onMount(() => {
        const savedData = localStorage.getItem('gameSetup');
        if (savedData) {
            const parsed = JSON.parse(savedData);
            theme = parsed.theme;
            subjectsInput = parsed.subjectsInput;
            numSpies = parsed.numSpies || 1; // Default to 1 spy if not provided
            spySubjects = parsed.spySubjects || [''];
            spyHints = parsed.spyHints || [''];
            showSubjectsToNonSpies = parsed.showSubjectsToNonSpies;
        }

        socket = createSocketStore('socket/admin');
        socket.onmessage = (event) => {
            players = JSON.parse(event.data).connectedPlayers;
        };
    });

    function handleAddSpy() {
        numSpies++;
        spySubjects.push('');
        spyHints.push('');
    }

    function handleRemoveSpy(index) {
        if (numSpies > 1) {
            numSpies--;
            spySubjects.splice(index, 1);
            spyHints.splice(index, 1);
        }
    }

    function handleSubmit() {
        const gameInput: GameInput = {
            messageType: "GameInput",
            theme,
            subjects: subjectsInput.split(',').map(s => s.trim()).filter(s => s),
            numSpies,
            spySubjects,
            spyHints,
            showSubjectsToNonSpies
        };

        socket.send(JSON.stringify(gameInput));
        isGameStarted = true;
        localStorage.removeItem('gameSetup');
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
<div class="form-group">
                    <label for="numSpies">Number of Spies</label>
                    <div class="spy-controls">
                        <button on:click={handleRemoveSpy} disabled={numSpies === 1}>-</button>
                        <span>{numSpies}</span>
                        <button on:click={handleAddSpy}>+</button>
                    </div>
                </div>

                {#each Array(numSpies) as _, i}
                    <div class="spy-inputs">
                        <div class="form-group">
                            <label for={`spySubject${i}`}>Spy {i + 1} Subject</label>
                            <input
                                type="text"
                                id={`spySubject${i}`}
                                bind:value={spySubjects[i]}
                                placeholder="Sunday"
                                required
                            />
                        </div>
                        <div class="form-group">
                            <label for={`spyHint${i}`}>Spy {i + 1} Hint</label>
                            <input
                                type="text"
                                id={`spyHint${i}`}
                                bind:value={spyHints[i]}
                                placeholder="Answer with something warm"
                                required
                            />
                        </div>
                    </div>
                {/each}

                <div class="form-group checkbox">
                    <label>
                        <input
                            type="checkbox"
                            bind:checked={showSubjectsToNonSpies}
                        />
                        Show subject list to non-spies
                    </label>
                </div>

                <button type="submit" class="submit-button" disabled={!isStartGameEnabled}>Start Game</button>
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

    .checkbox {
        display: flex;
        align-items: center;
    }

    .checkbox input {
        width: auto;
        margin-right: 0.5rem;
    }

    .checkbox label {
        margin-bottom: 0;
        display: flex;
        align-items: center;
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
    .spy-controls {
        display: flex;
        align-items: center;
    }

    .spy-controls button {
        padding: 0.25rem 0.5rem;
        font-size: 1.2rem;
    }

    .spy-controls span {
        margin: 0 0.5rem;
    }

    .spy-inputs {
        margin-bottom: 1rem;
    }
</style>