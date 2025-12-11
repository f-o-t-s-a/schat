export interface Message {
  id: number;
  content: string;
  sender: string;
  timestamp: Date;
  roomId?: string;
}

export interface MessageResponse {
  message: string;
  timestamp?: Date;
}
