# Artefatos do documento da APS

Esta pasta guarda, sob gerência de configuração, os artefatos do documento "APS – Projeto de software Web para autenticação de usuários" (Domínios de Software 2026/2). O código da aplicação fica nas pastas `backend/`, `frontend/`, `nginx/` e `banco/` da raiz.

| Pasta | Conteúdo | Parte do documento |
|---|---|---|
| `documento/` | PDF da versão atual do documento integrado; versões anteriores ficam no histórico do Git e nas *releases* | Documento inteiro |
| `documento/itens/` | Arquivos de trabalho dos itens de informação (.docx) | Seções 1 e 2 |
| `diagramas/` | Código-fonte PlantUML (`.puml`) e imagem (`.png`) das Figuras 1 a 10 | Apêndice I |
| `apendices/II-atas-e-comunicacoes/` | Prints e exportações citados nas atas e no registro de comunicações | Apêndice II |
| `apendices/III-prompts-ia/` | Prompts submetidos a ferramentas de IA, um arquivo por seção | Apêndice III |
| `apendices/I-plano/` | Guia da APS (plano de trabalho e matriz de autoria e revisão) | Citado no Apêndice II |
| `apendices/V-revisoes/` | Revisões dos itens, com os apontamentos e as correções | Citado no Apêndice II |

As pastas `I-plano` e `V-revisoes` mantêm esses nomes porque o Apêndice II do documento cita os caminhos `docs/apendices/I-plano/...` e `docs/apendices/V-revisoes/...`.

## Como gerar as figuras

Com o PlantUML instalado (ou o `plantuml.jar`):

```sh
cd docs/diagramas
java -jar plantuml.jar -charset UTF-8 -tpng *.puml
```

Ao alterar uma figura, altere o `.puml`, gere o `.png` de novo, atualize o código no Apêndice I e faça o commit dos dois arquivos juntos.

## Fluxo de trabalho

1. Cada tarefa tem uma issue no quadro Kanban do projeto.
2. O trabalho é feito numa branch própria (por exemplo, `fix/www-authenticate`).
3. O pull request cita a issue (`Closes #N`) e tem como revisor o revisor oficial do item; o autor nunca revisa o próprio trabalho.
4. Só o que foi revisado entra na `main`.
5. Cada PDF gerado para entrega vira uma *release* (por exemplo, `v1.0-entrega`), com o PDF anexado.
