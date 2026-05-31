import { contactsWsBaseUrl, refreshBrowserSession } from "@/api/client";
import type { LiveEvent } from "@/domain";

export interface WriteCommand {
  type?: "command";
  request_id: string;
  consumer_id: string;
  contract_name: string;
  payload: unknown;
}

export interface ReadSubscription {
  type?: "subscribe" | "unsubscribe";
  consumer_id: string;
  contracts: string[];
}

type MessageHandler = (message: LiveEvent) => void;
type StateHandler = (state: "connecting" | "open" | "closed" | "error") => void;

export class ContactsSocket {
  private socket: WebSocket | null = null;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  private reconnectAttempted = false;
  private shouldReconnect = false;
  private initialMessage?: ReadSubscription;
  private readonly onMessage: MessageHandler;
  private readonly onState: StateHandler;

  constructor(onMessage: MessageHandler, onState: StateHandler) {
    this.onMessage = onMessage;
    this.onState = onState;
  }

  connect(initialMessage?: ReadSubscription): void {
    this.disconnect();
    this.initialMessage = initialMessage;
    this.shouldReconnect = true;
    this.open();
  }

  private open(): void {
    this.onState("connecting");
    const socket = new WebSocket(contactsWsBaseUrl());
    this.socket = socket;
    socket.addEventListener("open", () => {
      this.reconnectAttempted = false;
      this.onState("open");
      if (this.initialMessage) this.subscribe(this.initialMessage);
    });
    socket.addEventListener("message", (event) => {
      try {
        this.onMessage(JSON.parse(event.data) as LiveEvent);
      } catch {
        this.onMessage({ type: "error", message: "Invalid WebSocket message" });
      }
    });
    socket.addEventListener("close", () => {
      if (this.socket !== socket) return;
      this.socket = null;
      this.onState("closed");
      this.reconnectAfterRefresh();
    });
    socket.addEventListener("error", () => this.onState("error"));
  }

  private reconnectAfterRefresh(): void {
    if (!this.shouldReconnect || this.reconnectAttempted) return;
    this.reconnectAttempted = true;
    void refreshBrowserSession()
      .then(() => {
        if (!this.shouldReconnect) return;
        this.reconnectTimer = setTimeout(() => this.open(), 250);
      })
      .catch(() => {
        this.shouldReconnect = false;
      });
  }

  send(payload: unknown): void {
    if (this.socket?.readyState === WebSocket.OPEN) {
      this.socket.send(JSON.stringify(payload));
    }
  }

  sendCommand(command: WriteCommand): void {
    this.send({ ...command, type: "command" });
  }

  subscribe(subscription: ReadSubscription): void {
    this.send({ ...subscription, type: "subscribe" });
  }

  unsubscribe(subscription: ReadSubscription): void {
    this.send({ ...subscription, type: "unsubscribe" });
  }

  disconnect(): void {
    this.shouldReconnect = false;
    if (this.reconnectTimer) clearTimeout(this.reconnectTimer);
    this.reconnectTimer = null;
    if (this.socket) {
      const socket = this.socket;
      this.socket = null;
      socket.close();
    }
  }
}
