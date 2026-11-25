import { inject, Injectable, LOCALE_ID } from '@angular/core';
import { MatDatepickerIntl } from '@angular/material/datepicker';

/** Calendar chrome (open / prev / next / …) in the UI locale, not the OS browser language. */
@Injectable()
export class LocalizedDatepickerIntl extends MatDatepickerIntl {
  constructor() {
    super();
    const locale = inject(LOCALE_ID);
    if (locale.startsWith('pt')) {
      this.calendarLabel = 'Calendário';
      this.openCalendarLabel = 'Abrir calendário';
      this.closeCalendarLabel = 'Fechar calendário';
      this.prevMonthLabel = 'Mês anterior';
      this.nextMonthLabel = 'Próximo mês';
      this.prevYearLabel = 'Ano anterior';
      this.nextYearLabel = 'Próximo ano';
      this.prevMultiYearLabel = '24 anos anteriores';
      this.nextMultiYearLabel = 'Próximos 24 anos';
      this.switchToMonthViewLabel = 'Escolher data';
      this.switchToMultiYearViewLabel = 'Escolher mês e ano';
    } else if (locale.startsWith('es')) {
      this.calendarLabel = 'Calendario';
      this.openCalendarLabel = 'Abrir calendario';
      this.closeCalendarLabel = 'Cerrar calendario';
      this.prevMonthLabel = 'Mes anterior';
      this.nextMonthLabel = 'Mes siguiente';
      this.prevYearLabel = 'Año anterior';
      this.nextYearLabel = 'Año siguiente';
      this.prevMultiYearLabel = '24 años anteriores';
      this.nextMultiYearLabel = 'Próximos 24 años';
      this.switchToMonthViewLabel = 'Elegir fecha';
      this.switchToMultiYearViewLabel = 'Elegir mes y año';
    }
    // en-US keeps MatDatepickerIntl English defaults.
    this.changes.next();
  }
}
