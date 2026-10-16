import React, { useState, useRef, useEffect, useCallback } from 'react';
import {
  Pen,
  Highlighter,
  Eraser,
  Square,
  Circle,
  Minus,
  MoveRight,
  Type,
  Hand,
  Undo2,
  Redo2,
  Trash2,
  ZoomIn,
  ZoomOut,
  Maximize2,
  Minimize2,
  ChevronLeft,
  ChevronRight,
  Plus,
  Layers,
  Settings,
  Video,
  Lock,
  Unlock,
  Share2,
  Download,
  Info,
  X,
  RotateCw,
  Play,
  Pause,
  RotateCcw,
  Smartphone,
  Eye,
  EyeOff,
  Image as ImageIcon
} from 'lucide-react';

// Data Types
type DrawingTool = 'pencil' | 'highlighter' | 'eraser' | 'line' | 'arrow' | 'rectangle' | 'circle' | 'text' | 'hand';
type BgType = 'whiteboard' | 'blackboard' | 'darkslate' | 'grid' | 'ruled' | 'image';
type AspectRatio = 'none' | '16:9' | '9:16';

interface Point {
  x: number; // 0..1 normalized to board width/height
  y: number;
}

interface StrokeElement {
  id: string;
  type: 'stroke';
  points: Point[];
  color: string;
  width: number;
  isHighlighter?: boolean;
}

interface ShapeElement {
  id: string;
  type: 'shape';
  shape: 'line' | 'arrow' | 'rectangle' | 'circle';
  startX: number;
  startY: number;
  endX: number;
  endY: number;
  color: string;
  width: number;
}

interface TextElement {
  id: string;
  type: 'text';
  text: string;
  x: number;
  y: number;
  color: string;
  fontSize: number;
  isBold?: boolean;
}

type CanvasElement = StrokeElement | ShapeElement | TextElement;

interface StudyPage {
  id: string;
  title: string;
  bgType: BgType;
  imageUrl?: string;
  rotation: number;
  elements: CanvasElement[];
}

interface StudySession {
  id: string;
  title: string;
  pages: StudyPage[];
  activePageIndex: number;
}

// Clean Monochrome & Educational Palette (No yellow UI theme)
const PALETTE_COLORS = [
  '#FFFFFF', // White
  '#000000', // Black
  '#EF4444', // Red
  '#38BDF8', // Sky Blue
  '#3B82F6', // Royal Blue
  '#10B981', // Green
  '#F97316', // Orange
  '#A855F7'  // Purple
];

// Clean initial state with ZERO pre-drafted dummy data
const DEFAULT_INITIAL_SESSIONS: StudySession[] = [
  {
    id: 'session_1',
    title: 'Study Board 1',
    activePageIndex: 0,
    pages: [
      {
        id: 'page_1',
        title: 'Slide 1',
        bgType: 'whiteboard',
        rotation: 0,
        elements: []
      }
    ]
  }
];

export default function App() {
  // Sessions & Pages state (Zero pre-drafted dummy data)
  const [sessions, setSessions] = useState<StudySession[]>(() => {
    try {
      const saved = localStorage.getItem('sainify_clean_sessions_v2');
      if (saved) {
        const parsed = JSON.parse(saved);
        if (Array.isArray(parsed) && parsed.length > 0) return parsed;
      }
    } catch (e) {
      console.error(e);
    }
    return DEFAULT_INITIAL_SESSIONS;
  });

  const [currentSessionId, setCurrentSessionId] = useState<string>(() => sessions[0]?.id || 'session_1');

  const currentSession = sessions.find(s => s.id === currentSessionId) || sessions[0];
  const activePageIndex = Math.min(Math.max(0, currentSession.activePageIndex), currentSession.pages.length - 1);
  const currentPage = currentSession.pages[activePageIndex] || currentSession.pages[0];

  // Active Tool & Style State
  const [currentTool, setCurrentTool] = useState<DrawingTool>('pencil');
  const [currentColor, setCurrentColor] = useState<string>('#000000');
  const [currentWidth, setCurrentWidth] = useState<number>(4);

  // Sync ref for immediate realtime drawing access without stale closures
  const currentColorRef = useRef<string>(currentColor);
  const currentWidthRef = useRef<number>(currentWidth);
  const currentToolRef = useRef<DrawingTool>(currentTool);

  useEffect(() => {
    currentColorRef.current = currentColor;
  }, [currentColor]);

  useEffect(() => {
    currentWidthRef.current = currentWidth;
  }, [currentWidth]);

  useEffect(() => {
    currentToolRef.current = currentTool;
  }, [currentTool]);

  // Master Elements Ref to fix "drawing a second stroke erases the first"
  const elementsRef = useRef<CanvasElement[]>(currentPage.elements || []);
  useEffect(() => {
    elementsRef.current = currentPage.elements || [];
  }, [currentPage.id, currentPage.elements]);

  // Undo / Redo Stacks
  const [undoStack, setUndoStack] = useState<CanvasElement[][]>([]);
  const [redoStack, setRedoStack] = useState<CanvasElement[][]>([]);

  // Canvas Viewport Transforms
  const [zoomScale, setZoomScale] = useState<number>(1);
  const [panOffset, setPanOffset] = useState<Point>({ x: 0, y: 0 });

  // UI Dock Visibility: Left Dock & Right Dock (collapsible to edge)
  const [isLeftDockOpen, setIsLeftDockOpen] = useState(true);
  const [isRightDockOpen, setIsRightDockOpen] = useState(true);
  const [isLocked, setIsLocked] = useState(false);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [aspectRatioGuide, setAspectRatioGuide] = useState<AspectRatio>('16:9'); // Default 16:9 landscape

  // Modal Dialogs
  const [showThumbnails, setShowThumbnails] = useState(false);
  const [showYouTubeStudio, setShowYouTubeStudio] = useState(false);
  const [showAbout, setShowAbout] = useState(false);
  const [showClearConfirm, setShowClearConfirm] = useState(false);
  const [showBgSelector, setShowBgSelector] = useState(false);
  const [showShapesPicker, setShowShapesPicker] = useState(false);
  const [textInputPos, setTextInputPos] = useState<Point | null>(null);
  const [textInputValue, setTextInputValue] = useState('');

  // YouTube Live Timer
  const [isTimerRunning, setIsTimerRunning] = useState(false);
  const [recordSeconds, setRecordSeconds] = useState(0);

  // Canvas & Interaction Refs
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const containerRef = useRef<HTMLDivElement | null>(null);
  const isPointerDownRef = useRef(false);
  const activeStrokeRef = useRef<Point[]>([]);
  const shapeStartRef = useRef<Point | null>(null);
  const dragStartScreenRef = useRef<Point | null>(null);

  // Auto-save sessions to localStorage
  useEffect(() => {
    localStorage.setItem('sainify_clean_sessions_v2', JSON.stringify(sessions));
  }, [sessions]);

  // Live Timer Effect
  useEffect(() => {
    let interval: any;
    if (isTimerRunning) {
      interval = setInterval(() => setRecordSeconds(s => s + 1), 1000);
    }
    return () => clearInterval(interval);
  }, [isTimerRunning]);

  // Fullscreen toggle handler (Hides system chrome & browser bars)
  const toggleFullscreen = () => {
    if (!document.fullscreenElement) {
      document.documentElement.requestFullscreen().catch(() => {});
      setIsFullscreen(true);
    } else {
      document.exitFullscreen().catch(() => {});
      setIsFullscreen(false);
    }
  };

  useEffect(() => {
    const onFsChange = () => {
      setIsFullscreen(!!document.fullscreenElement);
    };
    document.addEventListener('fullscreenchange', onFsChange);
    return () => document.removeEventListener('fullscreenchange', onFsChange);
  }, []);

  // Commit New Element & Update Page State Permanently
  const commitNewElement = useCallback((newElement: CanvasElement) => {
    const updated = [...elementsRef.current, newElement];
    elementsRef.current = updated;

    setUndoStack(prev => [...prev, elementsRef.current.slice(0, -1)]);
    setRedoStack([]);

    setSessions(prevSessions =>
      prevSessions.map(session => {
        if (session.id !== currentSession.id) return session;
        const newPages = session.pages.map((p, idx) => {
          if (idx !== activePageIndex) return p;
          return { ...p, elements: updated };
        });
        return { ...session, pages: newPages };
      })
    );
  }, [currentSession.id, activePageIndex]);

  // Bulk Replace Elements (e.g. for Eraser or Clear)
  const replaceElements = useCallback((newElements: CanvasElement[]) => {
    setUndoStack(prev => [...prev, elementsRef.current]);
    setRedoStack([]);
    elementsRef.current = newElements;

    setSessions(prevSessions =>
      prevSessions.map(session => {
        if (session.id !== currentSession.id) return session;
        const newPages = session.pages.map((p, idx) => {
          if (idx !== activePageIndex) return p;
          return { ...p, elements: newElements };
        });
        return { ...session, pages: newPages };
      })
    );
  }, [currentSession.id, activePageIndex]);

  // Undo / Redo
  const handleUndo = () => {
    if (undoStack.length === 0) return;
    const prevElements = undoStack[undoStack.length - 1];
    setRedoStack(r => [...r, elementsRef.current]);
    setUndoStack(u => u.slice(0, u.length - 1));
    elementsRef.current = prevElements;

    setSessions(prevSessions =>
      prevSessions.map(session => {
        if (session.id !== currentSession.id) return session;
        const newPages = session.pages.map((p, idx) => {
          if (idx !== activePageIndex) return p;
          return { ...p, elements: prevElements };
        });
        return { ...session, pages: newPages };
      })
    );
  };

  const handleRedo = () => {
    if (redoStack.length === 0) return;
    const nextElements = redoStack[redoStack.length - 1];
    setUndoStack(u => [...u, elementsRef.current]);
    setRedoStack(r => r.slice(0, r.length - 1));
    elementsRef.current = nextElements;

    setSessions(prevSessions =>
      prevSessions.map(session => {
        if (session.id !== currentSession.id) return session;
        const newPages = session.pages.map((p, idx) => {
          if (idx !== activePageIndex) return p;
          return { ...p, elements: nextElements };
        });
        return { ...session, pages: newPages };
      })
    );
  };

  // Canvas Rendering Routine
  const renderCanvas = useCallback(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const width = canvas.width;
    const height = canvas.height;

    ctx.save();
    ctx.clearRect(0, 0, width, height);

    // Apply Zoom & Pan
    ctx.translate(panOffset.x, panOffset.y);
    ctx.scale(zoomScale, zoomScale);

    // 1. Render Background
    switch (currentPage.bgType) {
      case 'whiteboard':
        ctx.fillStyle = '#FFFFFF';
        ctx.fillRect(0, 0, width, height);
        ctx.strokeStyle = '#E2E8F0';
        ctx.lineWidth = 1;
        ctx.strokeRect(0, 0, width, height);
        break;
      case 'blackboard':
        ctx.fillStyle = '#14281E'; // Chalkboard deep green
        ctx.fillRect(0, 0, width, height);
        break;
      case 'darkslate':
        ctx.fillStyle = '#0F172A';
        ctx.fillRect(0, 0, width, height);
        break;
      case 'grid':
        ctx.fillStyle = '#F8FAFC';
        ctx.fillRect(0, 0, width, height);
        ctx.strokeStyle = 'rgba(59, 130, 246, 0.15)';
        ctx.lineWidth = 1;
        const step = 30;
        for (let x = step; x < width; x += step) {
          ctx.beginPath();
          ctx.moveTo(x, 0);
          ctx.lineTo(x, height);
          ctx.stroke();
        }
        for (let y = step; y < height; y += step) {
          ctx.beginPath();
          ctx.moveTo(0, y);
          ctx.lineTo(width, y);
          ctx.stroke();
        }
        break;
      case 'ruled':
        ctx.fillStyle = '#FFFDF7';
        ctx.fillRect(0, 0, width, height);
        ctx.strokeStyle = 'rgba(100, 116, 139, 0.2)';
        ctx.lineWidth = 1;
        for (let y = 60; y < height; y += 36) {
          ctx.beginPath();
          ctx.moveTo(0, y);
          ctx.lineTo(width, y);
          ctx.stroke();
        }
        ctx.strokeStyle = 'rgba(239, 68, 68, 0.3)';
        ctx.lineWidth = 1.5;
        ctx.beginPath();
        ctx.moveTo(50, 0);
        ctx.lineTo(50, height);
        ctx.stroke();
        break;
      case 'image':
        if (currentPage.imageUrl) {
          const img = new Image();
          img.src = currentPage.imageUrl;
          if (img.complete) {
            ctx.drawImage(img, 0, 0, width, height);
          }
        } else {
          ctx.fillStyle = '#FFFFFF';
          ctx.fillRect(0, 0, width, height);
        }
        break;
    }

    // 2. Render all committed elements from elementsRef
    const elementsToDraw = elementsRef.current;
    elementsToDraw.forEach(elem => {
      if (elem.type === 'stroke') {
        if (elem.points.length < 1) return;
        ctx.save();
        ctx.strokeStyle = elem.color;
        ctx.lineWidth = elem.isHighlighter ? elem.width * 2.5 : elem.width;
        ctx.lineCap = 'round';
        ctx.lineJoin = 'round';
        if (elem.isHighlighter) {
          ctx.globalAlpha = 0.4;
        }

        ctx.beginPath();
        ctx.moveTo(elem.points[0].x * width, elem.points[0].y * height);
        for (let i = 1; i < elem.points.length; i++) {
          const prev = elem.points[i - 1];
          const curr = elem.points[i];
          const midX = ((prev.x + curr.x) / 2) * width;
          const midY = ((prev.y + curr.y) / 2) * height;
          ctx.quadraticCurveTo(prev.x * width, prev.y * height, midX, midY);
        }
        ctx.lineTo(elem.points[elem.points.length - 1].x * width, elem.points[elem.points.length - 1].y * height);
        ctx.stroke();
        ctx.restore();
      } else if (elem.type === 'shape') {
        const x1 = elem.startX * width;
        const y1 = elem.startY * height;
        const x2 = elem.endX * width;
        const y2 = elem.endY * height;

        ctx.save();
        ctx.strokeStyle = elem.color;
        ctx.lineWidth = elem.width;
        ctx.lineCap = 'round';

        if (elem.shape === 'line') {
          ctx.beginPath();
          ctx.moveTo(x1, y1);
          ctx.lineTo(x2, y2);
          ctx.stroke();
        } else if (elem.shape === 'arrow') {
          ctx.beginPath();
          ctx.moveTo(x1, y1);
          ctx.lineTo(x2, y2);
          ctx.stroke();

          const angle = Math.atan2(y2 - y1, x2 - x1);
          const headLen = Math.max(elem.width * 3.5, 14);
          const headAngle = (28 * Math.PI) / 180;

          ctx.fillStyle = elem.color;
          ctx.beginPath();
          ctx.moveTo(x2, y2);
          ctx.lineTo(x2 - headLen * Math.cos(angle - headAngle), y2 - headLen * Math.sin(angle - headAngle));
          ctx.lineTo(x2 - headLen * Math.cos(angle + headAngle), y2 - headLen * Math.sin(angle + headAngle));
          ctx.closePath();
          ctx.fill();
        } else if (elem.shape === 'rectangle') {
          const l = Math.min(x1, x2);
          const t = Math.min(y1, y2);
          const w = Math.abs(x2 - x1);
          const h = Math.abs(y2 - y1);
          ctx.strokeRect(l, t, w, h);
        } else if (elem.shape === 'circle') {
          const l = Math.min(x1, x2);
          const t = Math.min(y1, y2);
          const rx = Math.abs(x2 - x1) / 2;
          const ry = Math.abs(y2 - y1) / 2;
          ctx.beginPath();
          ctx.ellipse(l + rx, t + ry, rx, ry, 0, 0, Math.PI * 2);
          ctx.stroke();
        }
        ctx.restore();
      } else if (elem.type === 'text') {
        ctx.save();
        ctx.fillStyle = elem.color;
        ctx.font = `${elem.isBold ? 'bold ' : ''}${elem.fontSize}px sans-serif`;
        const lines = elem.text.split('\n');
        lines.forEach((line, idx) => {
          ctx.fillText(line, elem.x * width, elem.y * height + idx * (elem.fontSize * 1.3));
        });
        ctx.restore();
      }
    });

    // 3. Aspect Ratio Guide Framing (if chosen)
    if (aspectRatioGuide !== 'none') {
      const targetRatio = aspectRatioGuide === '16:9' ? 16 / 9 : 9 / 16;
      let guideW = width;
      let guideH = height;
      if (width / height > targetRatio) {
        guideW = height * targetRatio;
      } else {
        guideH = width / targetRatio;
      }
      const gx = (width - guideW) / 2;
      const gy = (height - guideH) / 2;

      ctx.save();
      ctx.strokeStyle = 'rgba(255, 255, 255, 0.5)';
      ctx.lineWidth = 1.5;
      ctx.setLineDash([8, 6]);
      ctx.strokeRect(gx, gy, guideW, guideH);
      ctx.restore();
    }

    ctx.restore();
  }, [currentPage, panOffset, zoomScale, aspectRatioGuide]);

  // Auto-resize canvas container to 16:9 landscape bounds
  useEffect(() => {
    const handleResize = () => {
      const canvas = canvasRef.current;
      const container = containerRef.current;
      if (!canvas || !container) return;

      const rect = container.getBoundingClientRect();
      const contW = rect.width;
      const contH = rect.height;

      // Fit 16:9 board comfortably in center
      const targetRatio = 16 / 9;
      let boardW = contW;
      let boardH = contH;

      if (contW / contH > targetRatio) {
        boardH = contH * 0.96;
        boardW = boardH * targetRatio;
      } else {
        boardW = contW * 0.96;
        boardH = boardW / targetRatio;
      }

      canvas.width = boardW;
      canvas.height = boardH;
      renderCanvas();
    };

    window.addEventListener('resize', handleResize);
    handleResize();
    return () => window.removeEventListener('resize', handleResize);
  }, [renderCanvas]);

  useEffect(() => {
    renderCanvas();
  }, [renderCanvas]);

  // Coordinate Conversion
  const getNormCoord = (e: React.PointerEvent<HTMLCanvasElement>): Point => {
    const canvas = canvasRef.current;
    if (!canvas) return { x: 0, y: 0 };
    const rect = canvas.getBoundingClientRect();
    const screenX = e.clientX - rect.left;
    const screenY = e.clientY - rect.top;

    const normX = (screenX - panOffset.x) / (canvas.width * zoomScale);
    const normY = (screenY - panOffset.y) / (canvas.height * zoomScale);

    return {
      x: Math.max(0, Math.min(1, normX)),
      y: Math.max(0, Math.min(1, normY))
    };
  };

  // Pointer Event Handlers
  const handlePointerDown = (e: React.PointerEvent<HTMLCanvasElement>) => {
    if (isLocked) return;
    isPointerDownRef.current = true;
    (e.target as HTMLElement).setPointerCapture(e.pointerId);

    const norm = getNormCoord(e);
    const tool = currentToolRef.current;

    if (tool === 'hand') {
      dragStartScreenRef.current = { x: e.clientX, y: e.clientY };
    } else if (tool === 'pencil' || tool === 'highlighter') {
      activeStrokeRef.current = [norm];
    } else if (['line', 'arrow', 'rectangle', 'circle'].includes(tool)) {
      shapeStartRef.current = norm;
    } else if (tool === 'eraser') {
      eraseAtPoint(norm);
    } else if (tool === 'text') {
      setTextInputPos(norm);
    }
  };

  const handlePointerMove = (e: React.PointerEvent<HTMLCanvasElement>) => {
    if (!isPointerDownRef.current) return;
    const tool = currentToolRef.current;

    if (tool === 'hand' && dragStartScreenRef.current) {
      const dx = e.clientX - dragStartScreenRef.current.x;
      const dy = e.clientY - dragStartScreenRef.current.y;
      dragStartScreenRef.current = { x: e.clientX, y: e.clientY };
      setPanOffset(prev => ({ x: prev.x + dx, y: prev.y + dy }));
      return;
    }

    const norm = getNormCoord(e);

    if (tool === 'pencil' || tool === 'highlighter') {
      activeStrokeRef.current.push(norm);
      renderCanvas();

      // Render live stroke in progress
      const canvas = canvasRef.current;
      const ctx = canvas?.getContext('2d');
      if (ctx && canvas) {
        ctx.save();
        ctx.translate(panOffset.x, panOffset.y);
        ctx.scale(zoomScale, zoomScale);
        ctx.strokeStyle = currentColorRef.current;
        ctx.lineWidth = tool === 'highlighter' ? currentWidthRef.current * 2.5 : currentWidthRef.current;
        ctx.lineCap = 'round';
        ctx.lineJoin = 'round';
        if (tool === 'highlighter') ctx.globalAlpha = 0.4;
        ctx.beginPath();
        const pts = activeStrokeRef.current;
        if (pts.length > 1) {
          ctx.moveTo(pts[0].x * canvas.width, pts[0].y * canvas.height);
          for (let i = 1; i < pts.length; i++) {
            ctx.lineTo(pts[i].x * canvas.width, pts[i].y * canvas.height);
          }
          ctx.stroke();
        }
        ctx.restore();
      }
    } else if (['line', 'arrow', 'rectangle', 'circle'].includes(tool) && shapeStartRef.current) {
      renderCanvas();
      const canvas = canvasRef.current;
      const ctx = canvas?.getContext('2d');
      if (ctx && canvas) {
        ctx.save();
        ctx.translate(panOffset.x, panOffset.y);
        ctx.scale(zoomScale, zoomScale);
        ctx.strokeStyle = currentColorRef.current;
        ctx.lineWidth = currentWidthRef.current;
        const x1 = shapeStartRef.current.x * canvas.width;
        const y1 = shapeStartRef.current.y * canvas.height;
        const x2 = norm.x * canvas.width;
        const y2 = norm.y * canvas.height;
        if (tool === 'line' || tool === 'arrow') {
          ctx.beginPath();
          ctx.moveTo(x1, y1);
          ctx.lineTo(x2, y2);
          ctx.stroke();
        } else if (tool === 'rectangle') {
          ctx.strokeRect(Math.min(x1, x2), Math.min(y1, y2), Math.abs(x2 - x1), Math.abs(y2 - y1));
        } else if (tool === 'circle') {
          const rx = Math.abs(x2 - x1) / 2;
          const ry = Math.abs(y2 - y1) / 2;
          ctx.beginPath();
          ctx.ellipse(Math.min(x1, x2) + rx, Math.min(y1, y2) + ry, rx, ry, 0, 0, Math.PI * 2);
          ctx.stroke();
        }
        ctx.restore();
      }
    } else if (tool === 'eraser') {
      eraseAtPoint(norm);
    }
  };

  const handlePointerUp = (e: React.PointerEvent<HTMLCanvasElement>) => {
    if (!isPointerDownRef.current) return;
    isPointerDownRef.current = false;
    dragStartScreenRef.current = null;

    const norm = getNormCoord(e);
    const tool = currentToolRef.current;

    if (tool === 'pencil' || tool === 'highlighter') {
      if (activeStrokeRef.current.length > 0) {
        const newStroke: StrokeElement = {
          id: 'str_' + Date.now() + Math.random(),
          type: 'stroke',
          points: [...activeStrokeRef.current],
          color: currentColorRef.current,
          width: currentWidthRef.current,
          isHighlighter: tool === 'highlighter'
        };
        commitNewElement(newStroke);
      }
      activeStrokeRef.current = [];
    } else if (['line', 'arrow', 'rectangle', 'circle'].includes(tool) && shapeStartRef.current) {
      const s = shapeStartRef.current;
      if (Math.hypot(norm.x - s.x, norm.y - s.y) > 0.005) {
        const newShape: ShapeElement = {
          id: 'shp_' + Date.now() + Math.random(),
          type: 'shape',
          shape: tool as any,
          startX: s.x,
          startY: s.y,
          endX: norm.x,
          endY: norm.y,
          color: currentColorRef.current,
          width: currentWidthRef.current
        };
        commitNewElement(newShape);
      }
      shapeStartRef.current = null;
    }
  };

  // Erase items hit by pointer
  const eraseAtPoint = (pt: Point) => {
    const threshold = 0.035;
    const current = elementsRef.current;
    const remaining = current.filter(elem => {
      if (elem.type === 'stroke') {
        return !elem.points.some(p => Math.hypot(p.x - pt.x, p.y - pt.y) < threshold);
      } else if (elem.type === 'shape') {
        const minX = Math.min(elem.startX, elem.endX) - threshold;
        const maxX = Math.max(elem.startX, elem.endX) + threshold;
        const minY = Math.min(elem.startY, elem.endY) - threshold;
        const maxY = Math.max(elem.startY, elem.endY) + threshold;
        return !(pt.x >= minX && pt.x <= maxX && pt.y >= minY && pt.y <= maxY);
      } else if (elem.type === 'text') {
        return Math.hypot(elem.x - pt.x, elem.y - pt.y) > threshold * 2;
      }
      return true;
    });

    if (remaining.length !== current.length) {
      replaceElements(remaining);
      renderCanvas();
    }
  };

  // Add New Slide
  const handleAddNewPage = (bg: BgType = 'whiteboard', imageUrl?: string) => {
    const newPage: StudyPage = {
      id: 'pg_' + Date.now(),
      title: imageUrl ? 'Imported Image' : `Slide ${currentSession.pages.length + 1}`,
      bgType: bg,
      imageUrl,
      rotation: 0,
      elements: []
    };

    setSessions(prev =>
      prev.map(s => {
        if (s.id !== currentSession.id) return s;
        return {
          ...s,
          pages: [...s.pages, newPage],
          activePageIndex: s.pages.length
        };
      })
    );
    setZoomScale(1);
    setPanOffset({ x: 0, y: 0 });
    setShowBgSelector(false);
  };

  // Import Images via File Picker
  const handleImageUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;

    Array.from(files).forEach(file => {
      const reader = new FileReader();
      reader.onload = ev => {
        if (ev.target?.result) {
          handleAddNewPage('image', ev.target.result as string);
        }
      };
      reader.readAsDataURL(file);
    });
  };

  // Export Slide as PNG
  const handleExportPNG = () => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const link = document.createElement('a');
    link.download = `Sainify_${currentPage.title}.png`;
    link.href = canvas.toDataURL('image/png');
    link.click();
  };

  return (
    <div className="relative flex h-screen w-screen overflow-hidden bg-black font-sans select-none text-white">
      {/* =========================================================================
          LEFT DOCK (Drawing Tools & Colors) - Anchored to Left Side, Never on Image
         ========================================================================= */}
      <div
        className={`fixed top-0 bottom-0 left-0 z-40 flex items-center transition-transform duration-300 ease-out pointer-events-none ${
          isLeftDockOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        <div className="pointer-events-auto ml-2 flex flex-col items-center gap-2.5 p-2 rounded-2xl bg-black/75 backdrop-blur-2xl border border-white/15 shadow-[0_8px_32px_rgba(0,0,0,0.8)] max-h-[96vh] overflow-y-auto">
          {/* Top Sainify Logo & Lock */}
          <div className="flex items-center gap-1.5 pb-1 border-b border-white/10 w-full justify-center">
            <div className="w-7 h-7 rounded-lg bg-white text-black font-black flex items-center justify-center text-sm shadow-sm">
              S
            </div>
            <button
              onClick={() => setIsLocked(!isLocked)}
              title={isLocked ? 'Locked' : 'Unlocked'}
              className={`p-1.5 rounded-lg transition-colors ${isLocked ? 'bg-white text-black' : 'text-white/60 hover:text-white'}`}
            >
              {isLocked ? <Lock className="w-3.5 h-3.5" /> : <Unlock className="w-3.5 h-3.5" />}
            </button>
          </div>

          {/* Primary Drawing Tools */}
          <div className="flex flex-col gap-1 w-full items-center">
            {/* Hand Pan Tool */}
            <button
              onClick={() => setCurrentTool('hand')}
              title="Pan & Zoom"
              className={`p-2.5 rounded-xl transition-all ${
                currentTool === 'hand' ? 'bg-white text-black font-bold shadow-md' : 'text-white/70 hover:text-white hover:bg-white/10'
              }`}
            >
              <Hand className="w-4 h-4" />
            </button>

            {/* Pen Tool */}
            <button
              onClick={() => setCurrentTool('pencil')}
              title="Freehand Pen"
              className={`p-2.5 rounded-xl transition-all ${
                currentTool === 'pencil' ? 'bg-white text-black font-bold shadow-md' : 'text-white/70 hover:text-white hover:bg-white/10'
              }`}
            >
              <Pen className="w-4 h-4" />
            </button>

            {/* Highlighter */}
            <button
              onClick={() => setCurrentTool('highlighter')}
              title="Highlighter"
              className={`p-2.5 rounded-xl transition-all ${
                currentTool === 'highlighter' ? 'bg-white text-black font-bold shadow-md' : 'text-white/70 hover:text-white hover:bg-white/10'
              }`}
            >
              <Highlighter className="w-4 h-4" />
            </button>

            {/* Eraser */}
            <button
              onClick={() => setCurrentTool('eraser')}
              title="Stroke Eraser"
              className={`p-2.5 rounded-xl transition-all ${
                currentTool === 'eraser' ? 'bg-white text-black font-bold shadow-md' : 'text-white/70 hover:text-white hover:bg-white/10'
              }`}
            >
              <Eraser className="w-4 h-4" />
            </button>

            {/* Shapes Picker Toggle */}
            <div className="relative">
              <button
                onClick={() => setShowShapesPicker(!showShapesPicker)}
                title="Geometric Shapes"
                className={`p-2.5 rounded-xl transition-all ${
                  ['line', 'arrow', 'rectangle', 'circle'].includes(currentTool)
                    ? 'bg-white text-black font-bold shadow-md'
                    : 'text-white/70 hover:text-white hover:bg-white/10'
                }`}
              >
                {currentTool === 'line' ? (
                  <Minus className="w-4 h-4" />
                ) : currentTool === 'arrow' ? (
                  <MoveRight className="w-4 h-4" />
                ) : currentTool === 'circle' ? (
                  <Circle className="w-4 h-4" />
                ) : (
                  <Square className="w-4 h-4" />
                )}
              </button>

              {/* Shapes Flyout Menu */}
              {showShapesPicker && (
                <div className="absolute left-12 top-0 bg-black/90 backdrop-blur-2xl border border-white/20 rounded-xl p-1.5 flex flex-col gap-1 shadow-2xl z-50">
                  <button
                    onClick={() => {
                      setCurrentTool('line');
                      setShowShapesPicker(false);
                    }}
                    className="flex items-center gap-2 p-1.5 hover:bg-white/15 rounded-lg text-xs"
                  >
                    <Minus className="w-3.5 h-3.5" /> Line
                  </button>
                  <button
                    onClick={() => {
                      setCurrentTool('arrow');
                      setShowShapesPicker(false);
                    }}
                    className="flex items-center gap-2 p-1.5 hover:bg-white/15 rounded-lg text-xs"
                  >
                    <MoveRight className="w-3.5 h-3.5" /> Arrow
                  </button>
                  <button
                    onClick={() => {
                      setCurrentTool('rectangle');
                      setShowShapesPicker(false);
                    }}
                    className="flex items-center gap-2 p-1.5 hover:bg-white/15 rounded-lg text-xs"
                  >
                    <Square className="w-3.5 h-3.5" /> Rectangle
                  </button>
                  <button
                    onClick={() => {
                      setCurrentTool('circle');
                      setShowShapesPicker(false);
                    }}
                    className="flex items-center gap-2 p-1.5 hover:bg-white/15 rounded-lg text-xs"
                  >
                    <Circle className="w-3.5 h-3.5" /> Circle
                  </button>
                </div>
              )}
            </div>

            {/* Text Tool */}
            <button
              onClick={() => setCurrentTool('text')}
              title="Text Annotation"
              className={`p-2.5 rounded-xl transition-all ${
                currentTool === 'text' ? 'bg-white text-black font-bold shadow-md' : 'text-white/70 hover:text-white hover:bg-white/10'
              }`}
            >
              <Type className="w-4 h-4" />
            </button>
          </div>

          <div className="w-full h-px bg-white/10 my-0.5" />

          {/* Color Palette (Realtime Switching) */}
          <div className="grid grid-cols-2 gap-1.5">
            {PALETTE_COLORS.map(color => {
              const isSelected = currentColor === color;
              return (
                <button
                  key={color}
                  onClick={() => {
                    setCurrentColor(color);
                    currentColorRef.current = color;
                  }}
                  style={{ backgroundColor: color }}
                  className={`w-4 h-4 rounded-full border transition-transform ${
                    isSelected ? 'ring-2 ring-white scale-125 border-black' : 'border-white/30 hover:scale-110'
                  }`}
                />
              );
            })}
          </div>

          <div className="w-full h-px bg-white/10 my-0.5" />

          {/* Stroke Width Selector */}
          <div className="flex flex-col gap-1 w-full items-center">
            {[2, 4, 8, 16].map(w => (
              <button
                key={w}
                onClick={() => {
                  setCurrentWidth(w);
                  currentWidthRef.current = w;
                }}
                className={`w-7 h-5 rounded text-[10px] font-mono font-bold flex items-center justify-center transition-colors ${
                  currentWidth === w ? 'bg-white text-black' : 'text-white/60 hover:text-white hover:bg-white/10'
                }`}
              >
                {w}
              </button>
            ))}
          </div>

          <div className="w-full h-px bg-white/10 my-0.5" />

          {/* Collapse Left Dock Button */}
          <button
            onClick={() => setIsLeftDockOpen(false)}
            title="Hide Left Toolbar"
            className="p-1.5 text-white/50 hover:text-white hover:bg-white/10 rounded-lg transition-colors"
          >
            <ChevronLeft className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Left Dock Reveal Handle (Always accessible on screen border when dock is closed) */}
      {!isLeftDockOpen && (
        <button
          onClick={() => setIsLeftDockOpen(true)}
          title="Show Tools"
          className="fixed left-0 top-1/2 -translate-y-1/2 z-40 p-2 py-4 bg-black/80 backdrop-blur-xl border-r border-y border-white/20 rounded-r-xl text-white hover:bg-white hover:text-black transition-all shadow-2xl"
        >
          <ChevronRight className="w-4 h-4" />
        </button>
      )}

      {/* =========================================================================
          RIGHT DOCK (Actions, Navigation, Export, YouTube) - Anchored to Right Side
         ========================================================================= */}
      <div
        className={`fixed top-0 bottom-0 right-0 z-40 flex items-center transition-transform duration-300 ease-out pointer-events-none ${
          isRightDockOpen ? 'translate-x-0' : 'translate-x-full'
        }`}
      >
        <div className="pointer-events-auto mr-2 flex flex-col items-center gap-2.5 p-2 rounded-2xl bg-black/75 backdrop-blur-2xl border border-white/15 shadow-[0_8px_32px_rgba(0,0,0,0.8)] max-h-[96vh] overflow-y-auto">
          {/* Collapse Right Dock Button */}
          <button
            onClick={() => setIsRightDockOpen(false)}
            title="Hide Right Actions"
            className="p-1.5 text-white/50 hover:text-white hover:bg-white/10 rounded-lg transition-colors pb-1 border-b border-white/10 w-full flex justify-center"
          >
            <ChevronRight className="w-4 h-4" />
          </button>

          {/* Undo & Redo */}
          <div className="flex flex-col gap-1 items-center">
            <button
              onClick={handleUndo}
              disabled={undoStack.length === 0}
              title="Undo"
              className="p-2 rounded-xl text-white disabled:text-white/20 hover:bg-white/10 transition-colors"
            >
              <Undo2 className="w-4 h-4" />
            </button>
            <button
              onClick={handleRedo}
              disabled={redoStack.length === 0}
              title="Redo"
              className="p-2 rounded-xl text-white disabled:text-white/20 hover:bg-white/10 transition-colors"
            >
              <Redo2 className="w-4 h-4" />
            </button>
          </div>

          <div className="w-full h-px bg-white/10 my-0.5" />

          {/* Clear Current Page Annotations */}
          <button
            onClick={() => setShowClearConfirm(true)}
            title="Clear Page"
            className="p-2 text-rose-400 hover:text-rose-300 hover:bg-white/10 rounded-xl transition-colors"
          >
            <Trash2 className="w-4 h-4" />
          </button>

          <div className="w-full h-px bg-white/10 my-0.5" />

          {/* Page Navigation */}
          <div className="flex flex-col gap-1 items-center">
            <button
              onClick={() =>
                setSessions(s =>
                  s.map(sess => (sess.id === currentSession.id ? { ...sess, activePageIndex: Math.max(0, activePageIndex - 1) } : sess))
                )
              }
              disabled={activePageIndex === 0}
              title="Previous Slide"
              className="p-2 rounded-xl text-white disabled:text-white/20 hover:bg-white/10 transition-colors"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>

            {/* Page Counter */}
            <span
              onClick={() => setShowThumbnails(true)}
              title="View Slides"
              className="text-[10px] font-mono font-bold text-white/90 bg-white/10 hover:bg-white/20 px-1.5 py-1 rounded cursor-pointer"
            >
              {activePageIndex + 1}/{currentSession.pages.length}
            </span>

            <button
              onClick={() =>
                setSessions(s =>
                  s.map(sess =>
                    sess.id === currentSession.id
                      ? { ...sess, activePageIndex: Math.min(currentSession.pages.length - 1, activePageIndex + 1) }
                      : sess
                  )
                )
              }
              disabled={activePageIndex === currentSession.pages.length - 1}
              title="Next Slide"
              className="p-2 rounded-xl text-white disabled:text-white/20 hover:bg-white/10 transition-colors"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>

          <div className="w-full h-px bg-white/10 my-0.5" />

          {/* Slide Management: Add Blank Slide / Import Image */}
          <div className="flex flex-col gap-1 items-center">
            {/* Add Slide */}
            <button
              onClick={() => setShowBgSelector(!showBgSelector)}
              title="Add New Slide"
              className="p-2 rounded-xl text-white hover:bg-white/10 transition-colors"
            >
              <Plus className="w-4 h-4" />
            </button>

            {/* Import Image */}
            <label title="Import Images" className="p-2 rounded-xl text-white hover:bg-white/10 transition-colors cursor-pointer">
              <ImageIcon className="w-4 h-4" />
              <input type="file" multiple accept="image/*" onChange={handleImageUpload} className="hidden" />
            </label>

            {/* Thumbnails Drawer */}
            <button
              onClick={() => setShowThumbnails(true)}
              title="All Slides Drawer"
              className="p-2 rounded-xl text-white hover:bg-white/10 transition-colors"
            >
              <Layers className="w-4 h-4" />
            </button>
          </div>

          <div className="w-full h-px bg-white/10 my-0.5" />

          {/* YouTube Studio & Aspect Frame */}
          <button
            onClick={() => setShowYouTubeStudio(true)}
            title="YouTube Teaching Studio"
            className="p-2 rounded-xl text-rose-500 hover:text-rose-400 hover:bg-white/10 transition-colors"
          >
            <Video className="w-4 h-4" />
          </button>

          {/* Immersive Fullscreen Mode */}
          <button
            onClick={toggleFullscreen}
            title={isFullscreen ? 'Exit Fullscreen' : 'Enter Fullscreen'}
            className="p-2 rounded-xl text-white hover:bg-white/10 transition-colors"
          >
            {isFullscreen ? <Minimize2 className="w-4 h-4" /> : <Maximize2 className="w-4 h-4" />}
          </button>

          {/* Export High-Res PNG */}
          <button
            onClick={handleExportPNG}
            title="Export Slide as PNG"
            className="p-2 rounded-xl text-white/80 hover:text-white hover:bg-white/10 transition-colors"
          >
            <Share2 className="w-4 h-4" />
          </button>

          {/* Download Android APK directly */}
          <a
            href="/Sainify.apk"
            download="Sainify.apk"
            title="Download Android APK"
            className="p-2 rounded-xl bg-white text-black font-bold hover:bg-white/90 transition-colors shadow-sm"
          >
            <Smartphone className="w-4 h-4" />
          </a>

          {/* About Dialog */}
          <button
            onClick={() => setShowAbout(true)}
            title="About Developer"
            className="p-1.5 text-white/40 hover:text-white rounded-lg transition-colors"
          >
            <Info className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>

      {/* Right Dock Reveal Handle (Always accessible on screen border when dock is closed) */}
      {!isRightDockOpen && (
        <button
          onClick={() => setIsRightDockOpen(true)}
          title="Show Actions"
          className="fixed right-0 top-1/2 -translate-y-1/2 z-40 p-2 py-4 bg-black/80 backdrop-blur-xl border-l border-y border-white/20 rounded-l-xl text-white hover:bg-white hover:text-black transition-all shadow-2xl"
        >
          <ChevronLeft className="w-4 h-4" />
        </button>
      )}

      {/* =========================================================================
          CENTRAL STUDY BOARD (Unobstructed 16:9 Landscape Canvas)
         ========================================================================= */}
      <div
        ref={containerRef}
        className="flex-1 w-full h-full flex items-center justify-center p-2 overflow-hidden bg-black"
      >
        <div className="relative rounded-2xl overflow-hidden border border-white/15 shadow-[0_0_60px_rgba(0,0,0,0.95)]">
          <canvas
            ref={canvasRef}
            onPointerDown={handlePointerDown}
            onPointerMove={handlePointerMove}
            onPointerUp={handlePointerUp}
            onPointerCancel={handlePointerUp}
            className="block touch-none cursor-crosshair"
          />

          {/* Zoom controls pinned quietly in lower canvas corner */}
          <div className="absolute bottom-2 right-2 flex items-center gap-1 bg-black/60 backdrop-blur-md px-2 py-1 rounded-lg border border-white/10 text-xs">
            <button
              onClick={() => setZoomScale(z => Math.max(0.5, z - 0.25))}
              className="text-white/60 hover:text-white px-1"
            >
              -
            </button>
            <span
              onClick={() => {
                setZoomScale(1);
                setPanOffset({ x: 0, y: 0 });
              }}
              className="font-mono text-[10px] text-white/80 cursor-pointer"
            >
              {Math.round(zoomScale * 100)}%
            </span>
            <button
              onClick={() => setZoomScale(z => Math.min(6, z + 0.25))}
              className="text-white/60 hover:text-white px-1"
            >
              +
            </button>
          </div>
        </div>
      </div>

      {/* =========================================================================
          MODALS & FLYOUTS
         ========================================================================= */}

      {/* New Slide Background Selector Flyout */}
      {showBgSelector && (
        <div className="fixed right-16 top-1/3 bg-black/90 backdrop-blur-2xl border border-white/20 rounded-2xl p-3 flex flex-col gap-2 z-50 shadow-2xl">
          <span className="text-[11px] font-bold text-white/80 tracking-wide uppercase px-1">Choose Slide Style</span>
          <button
            onClick={() => handleAddNewPage('whiteboard')}
            className="px-3 py-1.5 bg-white/10 hover:bg-white hover:text-black rounded-lg text-xs font-semibold text-left transition-colors"
          >
            Clean Whiteboard
          </button>
          <button
            onClick={() => handleAddNewPage('blackboard')}
            className="px-3 py-1.5 bg-white/10 hover:bg-white hover:text-black rounded-lg text-xs font-semibold text-left transition-colors"
          >
            Green Blackboard
          </button>
          <button
            onClick={() => handleAddNewPage('darkslate')}
            className="px-3 py-1.5 bg-white/10 hover:bg-white hover:text-black rounded-lg text-xs font-semibold text-left transition-colors"
          >
            Dark Slate
          </button>
          <button
            onClick={() => handleAddNewPage('grid')}
            className="px-3 py-1.5 bg-white/10 hover:bg-white hover:text-black rounded-lg text-xs font-semibold text-left transition-colors"
          >
            Graph Grid Paper
          </button>
          <button
            onClick={() => handleAddNewPage('ruled')}
            className="px-3 py-1.5 bg-white/10 hover:bg-white hover:text-black rounded-lg text-xs font-semibold text-left transition-colors"
          >
            Ruled Notebook
          </button>
        </div>
      )}

      {/* Slide Thumbnails Drawer */}
      {showThumbnails && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-md z-50 flex items-center justify-center p-6">
          <div className="w-full max-w-2xl bg-black/90 border border-white/20 rounded-3xl p-6 shadow-2xl flex flex-col gap-4">
            <div className="flex items-center justify-between pb-2 border-b border-white/10">
              <div>
                <h3 className="font-bold text-lg text-white">Lecture Slides & Images</h3>
                <p className="text-xs text-white/60">{currentSession.pages.length} slides in current session</p>
              </div>
              <button onClick={() => setShowThumbnails(false)} className="text-white/60 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="grid grid-cols-3 gap-4 max-h-[60vh] overflow-y-auto p-1">
              {currentSession.pages.map((p, idx) => (
                <div
                  key={p.id}
                  onClick={() => {
                    setSessions(s =>
                      s.map(sess => (sess.id === currentSession.id ? { ...sess, activePageIndex: idx } : sess))
                    );
                    setShowThumbnails(false);
                  }}
                  className={`flex flex-col items-center p-3 rounded-2xl cursor-pointer border transition-all ${
                    idx === activePageIndex
                      ? 'bg-white/15 border-white ring-2 ring-white/30'
                      : 'bg-white/5 border-white/10 hover:border-white/30'
                  }`}
                >
                  <div className="w-full h-24 rounded-xl bg-slate-900 border border-white/10 flex items-center justify-center relative overflow-hidden">
                    <span className="text-[10px] uppercase font-bold text-white/40">{p.bgType}</span>
                    <span className="absolute top-2 left-2 px-1.5 py-0.5 rounded bg-black/70 text-[10px] font-mono text-white">
                      {idx + 1}
                    </span>
                  </div>
                  <span className="text-xs font-semibold text-white/90 mt-2 truncate max-w-[140px]">{p.title}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* YouTube Teaching Studio Modal */}
      {showYouTubeStudio && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-md z-50 flex items-center justify-center p-6">
          <div className="w-full max-w-md bg-black/95 border border-white/20 rounded-3xl p-6 shadow-2xl flex flex-col gap-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2.5">
                <div className="w-9 h-9 rounded-xl bg-rose-600 flex items-center justify-center text-white font-bold">
                  <Video className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="font-bold text-base text-white">YouTube Teaching Studio</h3>
                  <p className="text-xs text-white/60">Framing guides & recording timer</p>
                </div>
              </div>
              <button onClick={() => setShowYouTubeStudio(false)} className="text-white/60 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Framing Guide Choice */}
            <div>
              <label className="text-xs font-bold text-white/80 block mb-2 uppercase tracking-wide">Video Framing</label>
              <div className="grid grid-cols-3 gap-2">
                {(['none', '16:9', '9:16'] as AspectRatio[]).map(r => (
                  <button
                    key={r}
                    onClick={() => setAspectRatioGuide(r)}
                    className={`py-2 px-3 rounded-xl text-xs font-bold border transition-all ${
                      aspectRatioGuide === r
                        ? 'bg-white text-black border-white shadow-lg'
                        : 'bg-white/5 border-white/10 text-white hover:bg-white/10'
                    }`}
                  >
                    {r === 'none' ? 'Full Canvas' : r === '16:9' ? '16:9 YouTube' : '9:16 Shorts'}
                  </button>
                ))}
              </div>
            </div>

            {/* Live Timer */}
            <div className="p-4 rounded-2xl bg-white/5 border border-white/10 flex flex-col items-center">
              <span className="text-3xl font-mono font-bold text-white tracking-widest">
                {String(Math.floor(recordSeconds / 60)).padStart(2, '0')}:
                {String(recordSeconds % 60).padStart(2, '0')}
              </span>
              <div className="flex items-center gap-2 mt-3">
                <button
                  onClick={() => setIsTimerRunning(!isTimerRunning)}
                  className={`flex items-center gap-1.5 px-4 py-2 rounded-xl text-xs font-bold transition-colors ${
                    isTimerRunning ? 'bg-amber-500 text-black' : 'bg-rose-600 text-white'
                  }`}
                >
                  {isTimerRunning ? <Pause className="w-3.5 h-3.5" /> : <Play className="w-3.5 h-3.5" />}
                  {isTimerRunning ? 'Pause Timer' : 'Start Timer'}
                </button>
                <button
                  onClick={() => {
                    setIsTimerRunning(false);
                    setRecordSeconds(0);
                  }}
                  className="p-2 text-white/60 hover:text-white rounded-xl bg-white/10"
                >
                  <RotateCcw className="w-4 h-4" />
                </button>
              </div>
            </div>

            <button
              onClick={() => {
                setShowYouTubeStudio(false);
                setIsLeftDockOpen(false);
                setIsRightDockOpen(false);
              }}
              className="w-full py-3 bg-white text-black font-bold rounded-xl text-xs hover:bg-white/90 transition-colors shadow-lg"
            >
              Hide All Controls & Begin Recording
            </button>
          </div>
        </div>
      )}

      {/* Clear Confirmation Modal */}
      {showClearConfirm && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-md z-50 flex items-center justify-center p-6">
          <div className="w-full max-w-sm bg-black/95 border border-white/20 rounded-3xl p-6 shadow-2xl flex flex-col gap-4 text-center">
            <h3 className="font-bold text-lg text-white">Clear Slide Annotations?</h3>
            <p className="text-xs text-white/70">
              All drawings and annotations on this slide will be cleared. The background remains intact.
            </p>
            <div className="flex items-center gap-3 mt-2">
              <button
                onClick={() => setShowClearConfirm(false)}
                className="flex-1 py-2.5 bg-white/10 hover:bg-white/20 text-white rounded-xl text-xs font-semibold"
              >
                Cancel
              </button>
              <button
                onClick={() => {
                  replaceElements([]);
                  renderCanvas();
                  setShowClearConfirm(false);
                }}
                className="flex-1 py-2.5 bg-rose-600 hover:bg-rose-500 text-white rounded-xl text-xs font-bold"
              >
                Clear Page
              </button>
            </div>
          </div>
        </div>
      )}

      {/* About Sainify Modal */}
      {showAbout && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-md z-50 flex items-center justify-center p-6">
          <div className="w-full max-w-sm bg-black/95 border border-white/20 rounded-3xl p-6 shadow-2xl flex flex-col items-center text-center gap-3">
            <div className="w-12 h-12 rounded-2xl bg-white text-black text-2xl font-black flex items-center justify-center shadow-lg">
              S
            </div>
            <h2 className="text-xl font-bold text-white tracking-wide">Sainify</h2>
            <p className="text-xs text-white/60 font-mono">Mobile-First Digital Study Board</p>

            <div className="w-full bg-white/5 rounded-2xl p-4 border border-white/10 text-left space-y-2.5 mt-2">
              <div className="text-xs">
                <span className="text-white/40 block text-[10px] uppercase font-bold tracking-wider">Developer Identity</span>
                <span className="font-semibold text-white">Ajay Saini</span>
              </div>
              <div className="text-xs">
                <span className="text-white/40 block text-[10px] uppercase font-bold tracking-wider">Brand Handle</span>
                <span className="font-semibold text-white">ajexweb</span>
              </div>
              <div className="text-xs">
                <span className="text-white/40 block text-[10px] uppercase font-bold tracking-wider">Instagram</span>
                <a
                  href="https://instagram.com/ajexweb"
                  target="_blank"
                  rel="noreferrer"
                  className="font-bold text-pink-400 hover:underline"
                >
                  @ajexweb
                </a>
              </div>
            </div>

            <button
              onClick={() => setShowAbout(false)}
              className="w-full py-2.5 bg-white text-black font-bold rounded-xl text-xs mt-2 hover:bg-white/90"
            >
              Close
            </button>
          </div>
        </div>
      )}

      {/* Text Annotation Dialog */}
      {textInputPos && (
        <div className="fixed inset-0 bg-black/70 backdrop-blur-md z-50 flex items-center justify-center p-6">
          <div className="w-full max-w-sm bg-black/95 border border-white/20 rounded-2xl p-4 shadow-2xl flex flex-col gap-3">
            <h4 className="font-bold text-sm text-white">Add Text Note</h4>
            <textarea
              value={textInputValue}
              onChange={e => setTextInputValue(e.target.value)}
              placeholder="Type your notes or formula..."
              rows={3}
              className="w-full bg-white/5 border border-white/15 rounded-xl p-3 text-xs text-white focus:outline-none focus:border-white resize-none"
              autoFocus
            />
            <div className="flex items-center gap-2 justify-end">
              <button
                onClick={() => {
                  setTextInputPos(null);
                  setTextInputValue('');
                }}
                className="px-3 py-1.5 text-xs text-white/60 hover:text-white"
              >
                Cancel
              </button>
              <button
                onClick={() => {
                  if (textInputValue.trim()) {
                    const newText: TextElement = {
                      id: 'txt_' + Date.now(),
                      type: 'text',
                      text: textInputValue.trim(),
                      x: textInputPos.x,
                      y: textInputPos.y,
                      color: currentColorRef.current,
                      fontSize: 20,
                      isBold: true
                    };
                    commitNewElement(newText);
                  }
                  setTextInputPos(null);
                  setTextInputValue('');
                }}
                className="px-4 py-1.5 bg-white text-black rounded-xl text-xs font-bold hover:bg-white/90"
              >
                Add Text
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
