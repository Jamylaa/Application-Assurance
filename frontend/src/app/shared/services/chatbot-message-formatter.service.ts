import { Injectable } from '@angular/core';
import { ChatbotResponse, ChatbotIntent } from '../../services/chatbot.service';

@Injectable({
  providedIn: 'root'
})
export class ChatbotMessageFormatterService {

  formatDataForDisplay(data: ChatbotResponse['data']): string {
    if (!data) return '';

    let formatted = '\n\n---\n\n';

    if (data.produit) {
      formatted += this.formatProduit(data.produit);
    }

    if (data.garantie) {
      formatted += this.formatGarantie(data.garantie);
    }

    if (data.pack) {
      formatted += this.formatPack(data.pack);
    }

    if (data.packGarantie) {
      formatted += this.formatPackGarantie(data.packGarantie);
    }

    if (data.recommendations) {
      formatted += this.formatRecommendations(data.recommendations);
    }

    if (data.topPacks) {
      formatted += this.formatTopPacks(data.topPacks);
    }

    if (data.topProduits) {
      formatted += this.formatTopProduits(data.topProduits);
    }

    if (data.criteria) {
      formatted += this.formatCriteria(data.criteria);
    }

    if (data.generatedPrompt) {
      formatted += `\nPrompt Gemini généré :\n${data.generatedPrompt}\n`;
    }

    return formatted;
  }

  private formatProduit(produit: any): string {
    return `Produit proposé :\n` +
           `• Nom : ${produit.nomProduit}\n` +
           `• Description : ${produit.description}\n` +
           `• Type : ${produit.typeProduit}\n` +
           `• Statut : ${produit.statut}\n`;
  }

  private formatGarantie(garantie: any): string {
    return `Garantie proposée :\n` +
           `• Nom : ${garantie.nomGarantie}\n` +
           `• Description : ${garantie.description}\n` +
           `• Taux : ${(garantie.tauxRemboursement * 100).toFixed(0)}%\n` +
           `• Statut : ${garantie.statut}\n`;
  }

  private formatPack(pack: any): string {
    return `Pack proposé :\n` +
           `• Nom : ${pack.nomPack}\n` +
           `• Description : ${pack.description}\n` +
           `• Prix : ${pack.prixMensuel}€/mois\n` +
           `• Niveau : ${pack.niveauCouverture}\n` +
           `• Statut : ${pack.statut}\n`;
  }

  private formatPackGarantie(pg: any): string {
    return `Association pack–garantie :\n` +
           `• Pack : ${pg.packId}\n` +
           `• Garantie : ${pg.garantieId}\n` +
           `• Taux : ${(Number(pg.tauxRemboursement) * 100).toFixed(0)}%\n` +
           `• Plafond : ${pg.plafond}\n`;
  }

  private formatRecommendations(recommendations: any[]): string {
    let formatted = 'Recommandations :\n';
    recommendations.forEach((rec, index) => {
      formatted += `\n${index + 1}. ${rec.title}\n`;
      formatted += `• Type : ${rec.type}\n`;
      formatted += `• Description : ${rec.description}\n`;
      if (rec.price) formatted += `• Prix : ${rec.price}€\n`;
      if (rec.features) formatted += `• Fonctionnalités : ${rec.features.join(', ')}\n`;
    });
    return formatted;
  }

  private formatTopPacks(topPacks: any[]): string {
    let formatted = 'Packs recommandés :\n';
    topPacks.forEach((pack, index) => {
      formatted += `\n${index + 1}. ${pack.nomPack} (score: ${pack.score})\n`;
      formatted += `• ID pack : ${pack.packId}\n`;
      formatted += `• Raisons : ${pack.reason}\n`;
    });
    return formatted;
  }

  private formatTopProduits(topProduits: any[]): string {
    let formatted = 'Produits recommandés :\n';
    topProduits.forEach((produit, index) => {
      formatted += `\n${index + 1}. ${produit.nomProduit} (score: ${produit.score})\n`;
      formatted += `• ID produit : ${produit.produitId}\n`;
      formatted += `• Raisons : ${produit.reason}\n`;
    });
    return formatted;
  }

  private formatCriteria(criteria: any): string {
    let formatted = 'Critères utilisés :\n';
    Object.entries(criteria).forEach(([key, value]) => {
      formatted += `• ${key} : ${JSON.stringify(value)}\n`;
    });
    return formatted;
  }

  formatMessage(text: string): string {
    return text.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
               .replace(/\n/g, '<br>');
  }

  getIntentIcon(intent?: ChatbotIntent): string {
    const icons: Record<ChatbotIntent, string> = {
      [ChatbotIntent.CREATE_PRODUIT]: 'bi-box',
      [ChatbotIntent.CREATE_GARANTIE]: 'bi-shield-check',
      [ChatbotIntent.CREATE_PACK]: 'bi-collection',
      [ChatbotIntent.CONFIGURE_PACK]: 'bi-gear',
      [ChatbotIntent.CREATE_PACK_WITH_GARANTIES]: 'bi-layers',
      [ChatbotIntent.UPDATE_PRODUIT]: 'bi-pencil',
      [ChatbotIntent.UPDATE_GARANTIE]: 'bi-pencil',
      [ChatbotIntent.UPDATE_PACK]: 'bi-pencil',
      [ChatbotIntent.DELETE_PRODUIT]: 'bi-trash',
      [ChatbotIntent.DELETE_GARANTIE]: 'bi-trash',
      [ChatbotIntent.DELETE_PACK]: 'bi-trash',
      [ChatbotIntent.LIST_PRODUITS]: 'bi-list',
      [ChatbotIntent.LIST_GARANTIES]: 'bi-list',
      [ChatbotIntent.LIST_PACKS]: 'bi-list',
      [ChatbotIntent.RECOMMENDATION]: 'bi-lightbulb',
      [ChatbotIntent.HELP]: 'bi-question-circle',
      [ChatbotIntent.UNKNOWN]: 'bi-question'
    };
    return icons[intent || ChatbotIntent.UNKNOWN] || 'bi-question';
  }

  getIntentLabel(intent?: ChatbotIntent): string {
    if (!intent) {
      return 'Assistant';
    }
    const labels: Record<ChatbotIntent, string> = {
      [ChatbotIntent.CREATE_PRODUIT]: 'Création produit',
      [ChatbotIntent.CREATE_GARANTIE]: 'Création garantie',
      [ChatbotIntent.CREATE_PACK]: 'Création pack',
      [ChatbotIntent.CONFIGURE_PACK]: 'Configuration pack',
      [ChatbotIntent.CREATE_PACK_WITH_GARANTIES]: 'Pack + garanties',
      [ChatbotIntent.UPDATE_PRODUIT]: 'Mise à jour produit',
      [ChatbotIntent.UPDATE_GARANTIE]: 'Mise à jour garantie',
      [ChatbotIntent.UPDATE_PACK]: 'Mise à jour pack',
      [ChatbotIntent.DELETE_PRODUIT]: 'Suppression produit',
      [ChatbotIntent.DELETE_GARANTIE]: 'Suppression garantie',
      [ChatbotIntent.DELETE_PACK]: 'Suppression pack',
      [ChatbotIntent.LIST_PRODUITS]: 'Liste produits',
      [ChatbotIntent.LIST_GARANTIES]: 'Liste garanties',
      [ChatbotIntent.LIST_PACKS]: 'Liste packs',
      [ChatbotIntent.RECOMMENDATION]: 'Recommandation',
      [ChatbotIntent.HELP]: 'Aide',
      [ChatbotIntent.UNKNOWN]: 'Général'
    };
    return labels[intent] || intent.replace(/_/g, ' ');
  }

  getIntentSeverity(intent?: ChatbotIntent): 'success' | 'info' | 'warning' | 'danger' | 'secondary' | 'contrast' {
    switch (intent) {
      case ChatbotIntent.CREATE_GARANTIE:
      case ChatbotIntent.CREATE_PRODUIT:
      case ChatbotIntent.CREATE_PACK:
        return 'success';
      case ChatbotIntent.CONFIGURE_PACK:
      case ChatbotIntent.CREATE_PACK_WITH_GARANTIES:
        return 'info';
      case ChatbotIntent.UPDATE_PRODUIT:
      case ChatbotIntent.UPDATE_GARANTIE:
      case ChatbotIntent.UPDATE_PACK:
        return 'warning';
      case ChatbotIntent.DELETE_PRODUIT:
      case ChatbotIntent.DELETE_GARANTIE:
      case ChatbotIntent.DELETE_PACK:
        return 'danger';
      case ChatbotIntent.RECOMMENDATION:
        return 'contrast';
      default:
        return 'secondary';
    }
  }

  getActionIcon(actionType: string): string {
    const icons: Record<string, string> = {
      'CREATE': 'bi-plus-circle',
      'UPDATE': 'bi-pencil',
      'DELETE': 'bi-trash',
      'NAVIGATE': 'bi-arrow-right',
      'LIST': 'bi-list'
    };
    return icons[actionType] || 'bi-circle';
  }

  getIntentExample(intentLabel: string): string {
    const examples: Record<string, string> = {
      'CREATE_PRODUIT': 'Créer un nouveau produit d\'assurance',
      'CREATE_GARANTIE': 'Créer une garantie hospitalisation',
      'CREATE_PACK': 'Créer un pack santé',
      'CONFIGURE_PACK': 'Configurer un pack avec garanties',
      'CREATE_PACK_WITH_GARANTIES': 'Créer un pack complet',
      'UPDATE_PRODUIT': 'Modifier un produit existant',
      'UPDATE_GARANTIE': 'Mettre à jour une garantie',
      'UPDATE_PACK': 'Modifier un pack',
      'DELETE_PRODUIT': 'Supprimer un produit',
      'DELETE_GARANTIE': 'Supprimer une garantie',
      'DELETE_PACK': 'Supprimer un pack',
      'LIST_PRODUITS': 'Lister tous les produits',
      'LIST_GARANTIES': 'Voir les garanties',
      'LIST_PACKS': 'Afficher les packs',
      'RECOMMENDATION': 'Recommander un produit',
      'HELP': 'Aide',
      'UNKNOWN': 'Aide'
    };
    return examples[intentLabel] || 'Aide';
  }
}
