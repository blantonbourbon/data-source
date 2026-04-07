import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ExternalIframeComponent } from './external-iframe.component';

@NgModule({
  declarations: [ExternalIframeComponent],
  imports: [CommonModule],
  exports: [ExternalIframeComponent]
})
export class ExternalIframeModule {}
