import { contactsWsBaseUrl, getStoredAccessToken } from '@/api/client';
import type { LiveEvent } from '@/domain';

export interface WriteCommand {
  type?: 'command';
  request_id: string;
  consumer_id: string;
  contract_name: string;
  payload: unknown;
}

export interface ReadSubscription {
  type?: 'subscribe' | 'unsubscribe';
  consumer_id: string;
  contracts: string[];
}

type MessageHandler = (message: LiveEvent) => void;
type StateHandler = (state: 'connecting' | 'open' | 'closed' | 'error') => void;

export class ContactsSocket {
  private socket: WebSocket | null = null;
  private readonly onMessage: MessageHandler;
  private readonly onState: StateHandler;

  constructor(
    onMessage: MessageHandler,
    onState: StateHandler,
  ) {
    this.onMessage = onMessage;
    this.onState = onState;
  }

  connect(initialMessage?: ReadSubscription): void {
    this.disconnect();
    this.onState('connecting');
    const url = new URL(contactsWsBaseUrl());
    const token = getStoredAccessToken();
    if (token) url.searchParams.set('access_token', token);
    this.socket = new WebSocket(url.toString());
    this.socket.addEventListener('open', () => {
      this.onState('open');
      if (initialMessage) this.subscribe(initialMessage);
    });
    this.socket.addEventListener('message', (event) => {
      try {
        this.onMessage(JSON.parse(event.data) as LiveEvent);
      } catch {
        this.onMessage({ type: 'error', message: 'Invalid WebSocket message' });
      }
    });
    this.socket.addEventListener('close', () => this.onState('closed'));
    this.socket.addEventListener('error', () => this.onState('error'));
  }

  send(payload: unknown): void {
    if (this.socket?.readyState === WebSocket.OPEN) {
      this.socket.send(JSON.stringify(payload));
    }
  }

  sendCommand(command: WriteCommand): void {
    this.send({ ...command, type: 'command' });
  }

  subscribe(subscription: ReadSubscription): void {
    this.send({ ...subscription, type: 'subscribe' });
  }

  unsubscribe(subscription: ReadSubscription): void {
    this.send({ ...subscription, type: 'unsubscribe' });
  }

  disconnect(): void {
    if (this.socket) {
      this.socket.close();
      this.socket = null;
    }
  }
}
