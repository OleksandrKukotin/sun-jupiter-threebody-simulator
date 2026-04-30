import { LagrangePoint, TrajectoryResult } from '../api/models';
import {
  AfterViewInit,
  Component,
  ElementRef,
  OnDestroy,
  ViewChild,
  effect,
  input,
} from '@angular/core';
import * as THREE from 'three';
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js';

const MU = 9.5368e-4;

@Component({
  selector: 'app-trajectory-3d',
  standalone: true,
  template: `<div #plotHost class="plot-host"></div>`,
  styles: [`
    :host {
      display: block;
      flex: 1;
      min-height: 0;
    }
    .plot-host {
      width: 100%;
      height: 100%;
    }
  `]
})
export class Trajectory3d implements AfterViewInit, OnDestroy {
  trajectoryResult = input<TrajectoryResult | null>(null);
  lagrangePoints = input<LagrangePoint[]>([]);

  @ViewChild('plotHost') private hostRef!: ElementRef<HTMLDivElement>;

  private scene!: THREE.Scene;
  private camera!: THREE.PerspectiveCamera;
  private renderer!: THREE.WebGLRenderer;
  private controls!: OrbitControls;
  private animationId = 0;
  private resizeObserver!: ResizeObserver;

  private readonly trajGroup = new THREE.Group();
  private readonly lpGroup = new THREE.Group();

  constructor() {
    effect(() => {
      const result = this.trajectoryResult();
      const lps = this.lagrangePoints();
      if (this.scene) this.rebuildDynamicObjects(result, lps);
    });
  }

  ngAfterViewInit(): void {
    this.initScene();
    this.buildStaticObjects();
    this.rebuildDynamicObjects(this.trajectoryResult(), this.lagrangePoints());
    this.animate();

    this.resizeObserver = new ResizeObserver(() => this.onResize());
    this.resizeObserver.observe(this.hostRef.nativeElement);
  }

  ngOnDestroy(): void {
    cancelAnimationFrame(this.animationId);
    this.resizeObserver?.disconnect();
    this.renderer?.dispose();
  }

  private initScene(): void {
    const host = this.hostRef.nativeElement;
    const w = host.clientWidth || 800;
    const h = host.clientHeight || 600;

    this.scene = new THREE.Scene();
    this.scene.background = new THREE.Color(0x0a0a1a);

    this.camera = new THREE.PerspectiveCamera(45, w / h, 0.001, 100);
    this.camera.position.set(0.5, 0, 3);

    this.renderer = new THREE.WebGLRenderer({ antialias: true });
    this.renderer.setPixelRatio(window.devicePixelRatio);
    this.renderer.setSize(w, h);
    host.appendChild(this.renderer.domElement);

    this.controls = new OrbitControls(this.camera, this.renderer.domElement);
    this.controls.enableDamping = true;
    this.controls.dampingFactor = 0.05;
    this.controls.target.set(0.5, 0, 0);

    this.scene.add(this.trajGroup, this.lpGroup);
    this.scene.add(new THREE.AmbientLight(0xffffff, 0.6));
    const dir = new THREE.DirectionalLight(0xffffff, 0.8);
    dir.position.set(2, 3, 4);
    this.scene.add(dir);

    const grid = new THREE.GridHelper(4, 20, 0x222244, 0x1a1a33);
    grid.rotation.x = Math.PI / 2;
    this.scene.add(grid);
  }

  private buildStaticObjects(): void {
    const sun = new THREE.Mesh(
      new THREE.SphereGeometry(0.04, 16, 16),
      new THREE.MeshPhongMaterial({ color: 0xffdd44, emissive: 0xffaa00, emissiveIntensity: 0.4 })
    );
    sun.position.set(-MU, 0, 0);
    this.scene.add(sun);

    const jupiter = new THREE.Mesh(
      new THREE.SphereGeometry(0.025, 16, 16),
      new THREE.MeshPhongMaterial({ color: 0xc88040 })
    );
    jupiter.position.set(1 - MU, 0, 0);
    this.scene.add(jupiter);
  }

  private rebuildDynamicObjects(result: TrajectoryResult | null, lps: LagrangePoint[]): void {
    this.clearGroup(this.trajGroup);
    this.clearGroup(this.lpGroup);
    if (result?.points.length) this.buildTrajectory(result);
    if (lps.length) this.buildLagrangePoints(lps);
  }

  private buildTrajectory(result: TrajectoryResult): void {
    const pts = result.points;
    const positions = new Float32Array(pts.length * 3);
    for (let i = 0; i < pts.length; i++) {
      positions[i * 3]     = pts[i].state.x;
      positions[i * 3 + 1] = pts[i].state.y;
      positions[i * 3 + 2] = 0;
    }
    const geo = new THREE.BufferGeometry();
    geo.setAttribute('position', new THREE.BufferAttribute(positions, 3));
    this.trajGroup.add(new THREE.Line(geo, new THREE.LineBasicMaterial({ color: 0x4488ff })));
  }

  private buildLagrangePoints(lps: LagrangePoint[]): void {
    const geo = new THREE.SphereGeometry(0.015, 8, 8);
    const mat = new THREE.MeshPhongMaterial({ color: 0x44ff88, emissive: 0x22aa44, emissiveIntensity: 0.3 });
    for (const lp of lps) {
      const mesh = new THREE.Mesh(geo, mat);
      mesh.position.set(lp.x, lp.y, 0);
      this.lpGroup.add(mesh);
    }
  }

  private clearGroup(group: THREE.Group): void {
    for (const child of group.children) {
      const obj = child as THREE.Mesh;
      if ('geometry' in obj) obj.geometry.dispose();
      if ('material' in obj) (obj.material as THREE.Material).dispose();
    }
    group.clear();
  }

  private animate(): void {
    this.animationId = requestAnimationFrame(() => this.animate());
    this.controls.update();
    this.renderer.render(this.scene, this.camera);
  }

  private onResize(): void {
    const host = this.hostRef.nativeElement;
    this.camera.aspect = host.clientWidth / host.clientHeight;
    this.camera.updateProjectionMatrix();
    this.renderer.setSize(host.clientWidth, host.clientHeight);
  }
}
