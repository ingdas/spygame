import { ReconnectingWebSocket } from '$lib/ReconnectingWebSocket';

export function createSocketStore(path) {
  const wsProtocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
  const wsHost = window.location.hostname === 'localhost' ? 'localhost:8089' : window.location.host;
  const ws = new ReconnectingWebSocket(`${wsProtocol}://${wsHost}/${path}`);
  return ws;
}