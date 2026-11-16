/**
 * Texts of Appendices A, B.1 and B.2 of specs/001-oracle-labeling/spec.md in the three locales, as
 * the item screen of spec 008 must show them, kept apart from the application code so the e2e
 * tests compare the screen against the specification and not against itself.
 */
export type Locale = 'pt-BR' | 'en-US' | 'es-ES';
export const LOCALES: readonly Locale[] = ['pt-BR', 'en-US', 'es-ES'];

export type Kind = 'MOD' | 'ADD' | 'DEL' | 'META';
export type Impact = 'LOW' | 'HIGH';
export type Operation = 'KEEP' | 'UPDATE' | 'REMOVE' | 'CREATE' | 'MERGE' | 'SPLIT' | 'REASSIGN' | 'FLAG';

export const KIND_LABELS: Record<Locale, Record<Kind, string>> = {
  'pt-BR': { MOD: 'Modificação', ADD: 'Inclusão', DEL: 'Remoção', META: 'Metadados' },
  'en-US': { MOD: 'Modification', ADD: 'Addition', DEL: 'Removal', META: 'Metadata' },
  'es-ES': { MOD: 'Modificación', ADD: 'Adición', DEL: 'Eliminación', META: 'Metadatos' },
};

export const FIELD_LABELS: Record<Locale, Record<string, string>> = {
  'pt-BR': {
    step: 'Passo do fluxo básico',
    steps_block: 'Bloco de passos do fluxo básico',
    alt_flow_step: 'Passo de fluxo alternativo',
    alt_flow_steps_block: 'Fluxo alternativo inteiro',
    alt_flow_title: 'Título de fluxo alternativo',
    exc_flow_step: 'Passo de fluxo de exceção',
    exc_flow_steps_block: 'Fluxo de exceção inteiro',
    exc_flow_title: 'Título de fluxo de exceção',
    pre: 'Pré-condição',
    post: 'Pós-condição',
    requirements: 'Vínculo com requisitos',
  },
  'en-US': {
    step: 'Basic flow step',
    steps_block: 'Block of basic flow steps',
    alt_flow_step: 'Alternative flow step',
    alt_flow_steps_block: 'Entire alternative flow',
    alt_flow_title: 'Alternative flow title',
    exc_flow_step: 'Exception flow step',
    exc_flow_steps_block: 'Entire exception flow',
    exc_flow_title: 'Exception flow title',
    pre: 'Precondition',
    post: 'Postcondition',
    requirements: 'Requirements link',
  },
  'es-ES': {
    step: 'Paso del flujo básico',
    steps_block: 'Bloque de pasos del flujo básico',
    alt_flow_step: 'Paso de flujo alternativo',
    alt_flow_steps_block: 'Flujo alternativo completo',
    alt_flow_title: 'Título de flujo alternativo',
    exc_flow_step: 'Paso de flujo de excepción',
    exc_flow_steps_block: 'Flujo de excepción completo',
    exc_flow_title: 'Título de flujo de excepción',
    pre: 'Precondición',
    post: 'Postcondición',
    requirements: 'Vínculo con requisitos',
  },
};

/** Option labels (Appendices B.1 and B.2). */
export const IMPACT_LABELS: Record<Locale, Record<Impact, string>> = {
  'pt-BR': { LOW: 'Baixo', HIGH: 'Alto' },
  'en-US': { LOW: 'Low', HIGH: 'High' },
  'es-ES': { LOW: 'Bajo', HIGH: 'Alto' },
};

export const OPERATION_LABELS: Record<Locale, Record<Operation, string>> = {
  'pt-BR': {
    KEEP: 'Manter',
    UPDATE: 'Atualizar',
    REMOVE: 'Remover',
    CREATE: 'Criar',
    MERGE: 'Unir',
    SPLIT: 'Dividir',
    REASSIGN: 'Revincular',
    FLAG: 'Sinalizar',
  },
  'en-US': {
    KEEP: 'Keep',
    UPDATE: 'Update',
    REMOVE: 'Remove',
    CREATE: 'Create',
    MERGE: 'Merge',
    SPLIT: 'Split',
    REASSIGN: 'Reassign',
    FLAG: 'Flag',
  },
  'es-ES': {
    KEEP: 'Mantener',
    UPDATE: 'Actualizar',
    REMOVE: 'Eliminar',
    CREATE: 'Crear',
    MERGE: 'Unir',
    SPLIT: 'Dividir',
    REASSIGN: 'Reasignar',
    FLAG: 'Señalar',
  },
};

/** Statement and explanation of each option, with the first letter capitalized as the screen shows it. */
export const IMPACT_STATEMENT: Record<Locale, string> = {
  'pt-BR': 'Esta edição muda o comportamento que o caso de uso especifica?',
  'en-US': 'Does this edit change the behavior that the use case specifies?',
  'es-ES': '¿Esta edición cambia el comportamiento que especifica el caso de uso?',
};

export const IMPACT_EXPLANATIONS: Record<Locale, Record<Impact, string>> = {
  'pt-BR': {
    LOW: 'A edição não muda o comportamento observável. Exemplos: reescrita, detalhamento, sinônimo, correção de digitação, pontuação, formatação, redução, reordenação, renomeação, ou mudança apenas no vínculo com requisitos. Um passo que só mudou de lugar, ou foi unido a outro ou separado dele, sem perder comportamento, também é baixo.',
    HIGH: 'A edição muda comportamento, entradas, saídas, condições ou o fluxo. Exemplos: incluir ou remover um requisito, funcionalidade, entidade, passo, fluxo alternativo ou de exceção. Também é alto quando o texto fica vago a ponto de o testador não conseguir decidir se o teste passou ou falhou.',
  },
  'en-US': {
    LOW: 'The edit does not change the observable behavior. Examples: rewording, added detail, synonym, typo fix, punctuation, formatting, shortening, reordering, renaming, or a change only in the requirements link. A step that was only moved, or merged with another or split from it, without losing behavior, is also low.',
    HIGH: 'The edit changes behavior, inputs, outputs, conditions or the flow. Examples: adding or removing a requirement, feature, entity, step, alternative or exception flow. It is also high when the text becomes so vague that the tester cannot decide whether the test passed or failed.',
  },
  'es-ES': {
    LOW: 'La edición no cambia el comportamiento observable. Ejemplos: reescritura, mayor detalle, sinónimo, corrección de erratas, puntuación, formato, reducción, reordenación, cambio de nombre, o un cambio solo en el vínculo con requisitos. Un paso que solo cambió de lugar, o que se unió a otro o se separó de él, sin perder comportamiento, también es bajo.',
    HIGH: 'La edición cambia el comportamiento, las entradas, las salidas, las condiciones o el flujo. Ejemplos: incluir o eliminar un requisito, una funcionalidad, una entidad, un paso, un flujo alternativo o de excepción. También es alto cuando el texto queda tan vago que quien prueba no puede decidir si la prueba pasó o falló.',
  },
};

export const OPERATION_STATEMENT: Record<Locale, string> = {
  'pt-BR': 'O que deve ser feito com os casos de teste manuais que exercitam este trecho?',
  'en-US': 'What should be done with the manual test cases that exercise this part?',
  'es-ES': '¿Qué se debe hacer con los casos de prueba manuales que ejercitan este fragmento?',
};

export const OPERATION_EXPLANATIONS: Record<Locale, Record<Operation, string>> = {
  'pt-BR': {
    KEEP: 'O caso de teste não exercita comportamento alterado, ou a edição só reescreve o texto; ele continua executável e verificável como está.',
    UPDATE:
      'O cenário continua existindo, mas um passo, dado, condição ou resultado esperado mudou, ou o resultado esperado precisa citar um detalhe novo.',
    REMOVE:
      'O comportamento que o caso de teste verifica deixou de existir (foi apagado, não movido nem unido a outro).',
    CREATE: 'Há comportamento novo que nenhum caso de teste existente cobre.',
    MERGE: 'O comportamento foi unido ao de outro caso de teste num único cenário.',
    SPLIT: 'Um único cenário virou vários cenários verificáveis de forma independente.',
    REASSIGN: 'O comportamento é o mesmo, mas o vínculo com requisitos mudou.',
    FLAG: 'A edição é ambígua ou deixa um passo vago; não há decisão segura e o caso precisa de análise humana.',
  },
  'en-US': {
    KEEP: 'The test case does not exercise changed behavior, or the edit only rewords the text; it remains executable and verifiable as it is.',
    UPDATE:
      'The scenario still exists, but a step, data item, condition or expected result changed, or the expected result needs to mention a new detail.',
    REMOVE: 'The behavior the test case verifies no longer exists (it was deleted, not moved or merged with another).',
    CREATE: 'There is new behavior that no existing test case covers.',
    MERGE: 'The behavior was merged with that of another test case into a single scenario.',
    SPLIT: 'A single scenario became several independently verifiable scenarios.',
    REASSIGN: 'The behavior is the same, but the requirements link changed.',
    FLAG: 'The edit is ambiguous or leaves a step vague; there is no safe decision and the case needs human analysis.',
  },
  'es-ES': {
    KEEP: 'El caso de prueba no ejercita comportamiento modificado, o la edición solo reescribe el texto; sigue siendo ejecutable y verificable tal como está.',
    UPDATE:
      'El escenario sigue existiendo, pero cambió un paso, un dato, una condición o el resultado esperado, o el resultado esperado debe mencionar un detalle nuevo.',
    REMOVE:
      'El comportamiento que verifica el caso de prueba dejó de existir (se borró; no se movió ni se unió a otro).',
    CREATE: 'Hay comportamiento nuevo que ningún caso de prueba existente cubre.',
    MERGE: 'El comportamiento se unió al de otro caso de prueba en un único escenario.',
    SPLIT: 'Un único escenario se convirtió en varios escenarios verificables de forma independiente.',
    REASSIGN: 'El comportamiento es el mismo, pero cambió el vínculo con requisitos.',
    FLAG: 'La edición es ambigua o deja un paso vago; no hay una decisión segura y el caso necesita análisis humano.',
  },
};

/** Sentence before the precedence order; the order itself is rendered with the localized option labels. */
export const PRECEDENCE_INTRO: Record<Locale, string> = {
  'pt-BR': 'Se mais de uma se aplicar, escolha a de maior consequência:',
  'en-US': 'If more than one applies, choose the one with the greatest consequence:',
  'es-ES': 'Si se aplica más de una, elige la de mayor consecuencia:',
};

export const PRECEDENCE_ORDER: readonly Operation[] = [
  'FLAG',
  'REMOVE',
  'MERGE',
  'SPLIT',
  'CREATE',
  'UPDATE',
  'REASSIGN',
  'KEEP',
];

/** The precedence rule as the screen shows it: the sentence and the localized labels joined by ">". */
export function precedenceText(locale: Locale): string {
  const labels = PRECEDENCE_ORDER.map((code) => OPERATION_LABELS[locale][code]);
  return `${PRECEDENCE_INTRO[locale]} ${labels.join(' > ')}`;
}

/** Stand-ins of FR-010. */
export const NONE: Record<Locale, string> = { 'pt-BR': '(nenhum)', 'en-US': '(none)', 'es-ES': '(ninguno)' };

export const MISSING_USECASE: Record<Locale, string> = {
  'pt-BR': '(este caso de uso não existe nesta versão)',
  'en-US': '(this use case does not exist in this version)',
  'es-ES': '(este caso de uso no existe en esta versión)',
};

/** Block titles, buttons and notes of the item screen (FR-008). */
export const SCREEN: Record<
  Locale,
  {
    before: string;
    after: string;
    added: string;
    removed: string;
    metaBefore: (field: string) => string;
    metaAfter: (field: string) => string;
    viewUsecase: (version: string) => string;
    previousTitle: (uc: string, version: string) => string;
    modifiedTitle: (uc: string, version: string) => string;
    close: string;
    noDifference: string;
  }
> = {
  'pt-BR': {
    before: 'Antes',
    after: 'Depois',
    added: 'Texto incluído',
    removed: 'Texto removido',
    metaBefore: (field) => `${field}: antes`,
    metaAfter: (field) => `${field}: depois`,
    viewUsecase: (version) => `Ver caso de uso completo (${version})`,
    previousTitle: (uc, version) => `${uc} · versão ${version} (anterior)`,
    modifiedTitle: (uc, version) => `${uc} · versão ${version} (modificada)`,
    close: 'Fechar',
    noDifference: 'Nenhuma diferença de texto nesta parte',
  },
  'en-US': {
    before: 'Before',
    after: 'After',
    added: 'Added text',
    removed: 'Removed text',
    metaBefore: (field) => `${field}: before`,
    metaAfter: (field) => `${field}: after`,
    viewUsecase: (version) => `View full use case (${version})`,
    previousTitle: (uc, version) => `${uc} · version ${version} (previous)`,
    modifiedTitle: (uc, version) => `${uc} · version ${version} (modified)`,
    close: 'Close',
    noDifference: 'No text difference in this part',
  },
  'es-ES': {
    before: 'Antes',
    after: 'Después',
    added: 'Texto añadido',
    removed: 'Texto eliminado',
    metaBefore: (field) => `${field}: antes`,
    metaAfter: (field) => `${field}: después`,
    viewUsecase: (version) => `Ver caso de uso completo (${version})`,
    previousTitle: (uc, version) => `${uc} · versión ${version} (anterior)`,
    modifiedTitle: (uc, version) => `${uc} · versión ${version} (modificada)`,
    close: 'Cerrar',
    noDifference: 'Ninguna diferencia de texto en esta parte',
  },
};
