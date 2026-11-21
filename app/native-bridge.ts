import type {SystemId} from './anatomy';

export interface NativeStructurePayload {
 id: string;
 name: string;
 system: SystemId;
}

export interface AnatomyBridgeApi {
 onStructureSelected?: (payloadJson: string) => void;
 onSelectionCleared?: () => void;
}

declare global {
 interface Window {
  AnatomyBridge?: AnatomyBridgeApi;
  focusStructure?: (id: string) => boolean;
  resetCamera?: () => void;
 }
}

export function notifyStructureSelected(payload: NativeStructurePayload): void {
 try {
  window.AnatomyBridge?.onStructureSelected?.(JSON.stringify(payload));
 } catch {
  // Ignore bridge invocation errors in standard browser contexts.
 }
}

export function notifySelectionCleared(): void {
 try {
  window.AnatomyBridge?.onSelectionCleared?.();
 } catch {
  // Ignore bridge invocation errors in standard browser contexts.
 }
}

export function registerNativeControls(handlers: {
 focusStructure: (id: string) => boolean;
 resetCamera: () => void;
}): () => void {
 window.focusStructure = handlers.focusStructure;
 window.resetCamera = handlers.resetCamera;
 return () => {
  if (window.focusStructure === handlers.focusStructure) {
   delete window.focusStructure;
  }
  if (window.resetCamera === handlers.resetCamera) {
   delete window.resetCamera;
  }
 };
}
