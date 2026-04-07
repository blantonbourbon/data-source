import { Component, Input, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

@Component({
  standalone: false,
  selector: 'app-external-iframe',
  templateUrl: './external-iframe.component.html',
  styleUrl: './external-iframe.component.scss'
})
export class ExternalIframeComponent implements OnInit, OnChanges {
  @Input() url: string = 'https://angular.dev/';
  iframeUrl: SafeResourceUrl | undefined;

  constructor(private sanitizer: DomSanitizer) {}

  ngOnInit() {
    this.updateIframeUrl();
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['url']) {
      this.updateIframeUrl();
    }
  }

  private updateIframeUrl() {
    if (this.url) {
      this.iframeUrl = this.sanitizer.bypassSecurityTrustResourceUrl(this.url);
    }
  }
}
