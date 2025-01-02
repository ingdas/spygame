import ReconnectingWebSocket from 'reconnecting-websocket';

export function createSocketStore(path: string) {
	const wsProtocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
	const wsHost = window.location.hostname === 'localhost' ? 'localhost:8089' : window.location.host;
	return new ReconnectingWebSocket(`${wsProtocol}://${wsHost}/${path}`);
}
