import { post } from '@/utils/request';
import useLocale from '@/utils/useLocale';
import {
  Button,
  Image as ArcoImage,
  Message,
  Modal,
  Space,
  Tooltip,
  Upload,
} from '@arco-design/web-react';
import {
  IconDelete,
  IconEdit,
  IconEye,
  IconPlus,
} from '@arco-design/web-react/icon';
import { useControllableValue } from 'ahooks';
import React, { useRef, useState } from 'react';
import Cropper, { ReactCropperProps } from 'react-cropper';

import 'cropperjs/dist/cropper.css';

interface Size {
  width: number;
  height: number;
}
interface CropImageUploadProps {
  cropSize?: Size;
  value?: string;
  onChange?: (value: string) => void;
  onAdd?: () => void;
  onDel?: () => void;
  disabled?: boolean;
}
const CropImageUpload = ({
  value,
  onChange,
  onAdd,
  onDel,
  disabled,
}: CropImageUploadProps) => {
  const dt = useLocale();
  const cropImageRef = useRef<any>();
  const [src, setSrc] = useControllableValue({ value, onChange });

  const [preview, setPreview] = useState(false);
  const [visible, setVisible] = useState(false);

  const [image, setImage] = useState('');

  const handleBeforeUpload = (file: File) => {
    const reader = new FileReader();
    const image = new Image();
    reader.readAsDataURL(file);
    reader.onload = (e) => {
      image.src = reader.result.toString();
      image.onload = () => {
        setImage(e.target.result.toString());
        setVisible(true);
      };
    };
    return false;
  };
  const handleSubmit = async () => {
    const data = new FormData();
    data.append('file', cropImageRef.current);
    try {
      const { id } = await post('/api/blobs/upload', data, {
        headers: { 'content-type': 'multipart/form-data' },
      });
      setSrc(`/api/blobs/get/${id}`);
      setVisible(false);
      Message.success(dt['common.upload.success']);
    } catch (e) {
      Message.error(dt['common.upload.error']);
    }
    return;
  };

  const cropperRef = useRef<any>(null);
  const onCrop = async () => {
    const imageElement: any = cropperRef?.current;
    const cropper: any = imageElement?.cropper;
    await cropper
      .getCroppedCanvas({
        imageSmoothingQuality: 'high',
        fillColor: 'transparent',
      })
      .toBlob((_blob) => {
        cropImageRef.current = _blob;
      }, 'image/png');
  };
  const onReady: ReactCropperProps['ready'] = (evt) => {
    const taget: any = evt.target;
    const cropper: any = taget.cropper;
    const { width, height } = cropper.getContainerData();
    cropper.setCropBoxData({ height, width });
  };

  return (
    <div className="arco-upload-trigger">
      <Upload
        showUploadList={false}
        style={{ width: '100%', height: 'max-content' }}
        disabled={disabled}
      >
        <div
          className="arco-upload-list-item-picture"
          style={{ width: '100%', height: 'max-content' }}
          onClick={(evt) => {
            evt.stopPropagation();
          }}
        >
          <ArcoImage
            width="100%"
            height={288.75}
            src={src || ''}
            previewProps={{
              visible: preview,
              onVisibleChange: setPreview,
            }}
          />
          {!disabled && (
            <div className="arco-upload-list-item-picture-mask">
              <Space className="h-full">
                <Tooltip content={dt['image.tooltip.preview']}>
                  <Button
                    type="primary"
                    shape="circle"
                    icon={<IconEye />}
                    onClick={() => setPreview(true)}
                  />
                </Tooltip>
                {onAdd && (
                  <Tooltip content={dt['image.tooltip.add']}>
                    <Button
                      type="primary"
                      shape="circle"
                      status="success"
                      icon={<IconPlus />}
                      onClick={onAdd}
                    />
                  </Tooltip>
                )}
                <Tooltip content={dt['image.tooltip.select']}>
                  <Upload
                    key="upload-image"
                    accept="image/*"
                    action="/api/blobs/upload"
                    showUploadList={false}
                    beforeUpload={handleBeforeUpload}
                  >
                    <Button
                      type="primary"
                      shape="circle"
                      status="warning"
                      icon={<IconEdit />}
                    />
                  </Upload>
                </Tooltip>

                {onDel && (
                  <Tooltip content={dt['image.tooltip.delete']}>
                    <Button
                      type="primary"
                      shape="circle"
                      status="danger"
                      icon={<IconDelete />}
                      onClick={onDel}
                    />
                  </Tooltip>
                )}
              </Space>
            </div>
          )}
        </div>
      </Upload>
      {!disabled && (
        <Modal
          visible={visible}
          title={dt['image.crop']}
          style={{ width: 820 }}
          onCancel={() => setVisible(false)}
          onOk={handleSubmit}
          unmountOnExit
        >
          <div style={{ height: 520 }}>
            <Cropper
              src={image}
              style={{ height: 520, width: '100%' }}
              guides={false}
              crop={onCrop}
              ref={cropperRef}
              autoCropArea={1}
              initialAspectRatio={16/9}
              viewMode={1}
              minCropBoxHeight={20}
              minCropBoxWidth={20}
              responsive
              checkOrientation={false}
              ready={onReady}
            />
          </div>
        </Modal>
      )}
    </div>
  );
};

export default CropImageUpload;
