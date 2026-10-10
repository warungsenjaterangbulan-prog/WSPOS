import React, { useEffect, useRef } from 'react';
import JsBarcode from 'jsbarcode';

interface BarcodeDisplayProps {
  value: string;
  width?: number;
  height?: number;
  displayValue?: boolean;
  className?: string;
}

export const BarcodeDisplay: React.FC<BarcodeDisplayProps> = ({
  value,
  width = 1.6,
  height = 42,
  displayValue = true,
  className = '',
}) => {
  const svgRef = useRef<SVGSVGElement | null>(null);

  useEffect(() => {
    if (!svgRef.current || !value) return;
    try {
      JsBarcode(svgRef.current, value, {
        format: 'CODE128',
        width,
        height,
        displayValue,
        fontSize: 12,
        margin: 4,
        background: '#ffffff',
        lineColor: '#1c1917',
      });
    } catch {
      // Fallback if barcode value has unsupported chars
    }
  }, [value, width, height, displayValue]);

  if (!value) return null;

  return (
    <div className={`inline-flex flex-col items-center rounded-lg bg-white p-1.5 ${className}`}>
      <svg ref={svgRef} className="max-w-full" />
    </div>
  );
};
