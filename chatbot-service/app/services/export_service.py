from fpdf import FPDF
from fpdf.enums import XPos, YPos

from app.models.history_schemas import ConversationDTO


def _safe_latin1(text: str) -> str:
    """Les polices de base fpdf2 (Helvetica) sont limitées à Latin-1 — un caractère hors
    de cette plage (emoji, etc.) ferait échouer tout l'export. On le remplace plutôt que
    de faire planter la génération pour un message par ailleurs valide."""
    return (text or "").encode("latin-1", errors="replace").decode("latin-1")


def _line(pdf: FPDF, w: float, h: float, text: str) -> None:
    """multi_cell qui revient systématiquement à la marge gauche sur la ligne suivante —
    le défaut de fpdf2 (new_x=RIGHT, new_y=TOP) laisse le curseur au milieu de la ligne
    courante, ce qui casse tout appel width=0 suivant (largeur restante négative)."""
    pdf.multi_cell(w, h, text, new_x=XPos.LMARGIN, new_y=YPos.NEXT)


def generate_pdf(conversation: ConversationDTO) -> bytes:
    pdf = FPDF()
    pdf.add_page()
    pdf.set_font("Helvetica", "B", 16)
    _line(pdf, 0, 10, _safe_latin1(conversation.title or "Conversation"))
    pdf.set_font("Helvetica", "", 9)
    pdf.set_text_color(100, 100, 100)
    _line(pdf, 0, 6, _safe_latin1(f"Créée le {conversation.created_at:%d/%m/%Y %H:%M}"))
    _line(pdf, 0, 6, _safe_latin1(f"{len(conversation.messages)} message(s)"))
    pdf.ln(4)
    pdf.set_text_color(0, 0, 0)

    for msg in conversation.messages:
        sender = "Vous" if msg.sender == "user" else "Assistant"
        pdf.set_font("Helvetica", "B", 10)
        _line(pdf, 0, 6, _safe_latin1(f"{sender} — {msg.timestamp:%d/%m/%Y %H:%M}"))
        pdf.set_font("Helvetica", "", 10)
        _line(pdf, 0, 6, _safe_latin1(msg.text))
        pdf.ln(3)

    return bytes(pdf.output())


def generate_text(conversation: ConversationDTO) -> str:
    lines = [
        conversation.title or "Conversation",
        "=" * 60,
        f"Créée le {conversation.created_at:%d/%m/%Y %H:%M}",
        f"{len(conversation.messages)} message(s)",
        "",
    ]
    for msg in conversation.messages:
        sender = "VOUS" if msg.sender == "user" else "ASSISTANT"
        lines.append(f"[{msg.timestamp:%d/%m/%Y %H:%M}] {sender}:")
        lines.append(msg.text)
        lines.append("")
    return "\n".join(lines)
