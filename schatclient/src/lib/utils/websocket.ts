import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const WS_URL = process.env.NEXT_PUBLIC_WS_URL || 'http://localhost:8080';

export class WebSocketService {
  private client: Client | null = null;
  private connected: boolean = false;

  connect(token: string, onMessageReceived: (message: any) => void): void {
    if (this.connected) {
      console.log('Already connected to WebSocket');
      return;
    }

    this.client = new Client({
      webSocketFactory: () => new SockJS(`${WS_URL}/ws`),
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      debug: (str) => {
        console.log('STOMP Debug:', str);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    this.client.onConnect = () => {
      console.log('Connected to WebSocket');
      this.connected = true;

      // Subscribe to topics
      this.client?.subscribe('/topic/messages', (message: IMessage) => {
        const receivedMessage = JSON.parse(message.body);
        onMessageReceived(receivedMessage);
      });

      this.client?.subscribe('/user/queue/private', (message: IMessage) => {
        const receivedMessage = JSON.parse(message.body);
        onMessageReceived(receivedMessage);
      });
    };

    this.client.onStompError = (frame) => {
      console.error('STOMP error:', frame);
      this.connected = false;
    };

    this.client.onDisconnect = () => {
      console.log('Disconnected from WebSocket');
      this.connected = false;
    };

    this.client.activate();
  }

  sendMessage(destination: string, message: any): void {
    if (this.client && this.connected) {
      this.client.publish({
        destination,
        body: JSON.stringify(message),
      });
    } else {
      console.error('WebSocket is not connected');
    }
  }

  disconnect(): void {
    if (this.client) {
      this.client.deactivate();
      this.connected = false;
    }
  }

  isConnected(): boolean {
    return this.connected;
  }
}

export const webSocketService = new WebSocketService();
