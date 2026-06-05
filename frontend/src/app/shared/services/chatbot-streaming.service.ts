import { Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ChatbotStreamingService {

  async streamMessage(
    message: string,
    onChunk: (chunk: string) => void,
    onComplete: () => void,
    onError: (error: Error) => void
  ): Promise<void> {
    const token = localStorage.getItem('token');
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      'Accept': 'text/event-stream'
    };

    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    try {
      const response = await fetch(`${environment.apiProduit}/chatbot/stream/chat`, {
        method: 'POST',
        headers,
        body: JSON.stringify({ message })
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const reader = response.body?.getReader();
      const decoder = new TextDecoder();

      if (!reader) {
        throw new Error('Response body is null');
      }

      let fullResponse = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        const chunk = decoder.decode(value);
        const lines = chunk.split('\n');

        for (const line of lines) {
          if (line.startsWith('data: ')) {
            const data = line.substring(6);
            if (data && data !== '[DONE]' && !data.startsWith('Error:')) {
              fullResponse += data;
              onChunk(fullResponse);
            }
          }
        }
      }

      onComplete();
    } catch (error) {
      onError(error as Error);
    }
  }

  isBusinessOperation(message: string): boolean {
    const lowerMessage = message.toLowerCase();
    return lowerMessage.includes('créer') ||
           lowerMessage.includes('create') ||
           lowerMessage.includes('ajouter') ||
           lowerMessage.includes('nouveau') ||
           lowerMessage.includes('configurer');
  }
}
