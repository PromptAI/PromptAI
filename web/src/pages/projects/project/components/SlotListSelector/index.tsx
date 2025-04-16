import * as React from "react";
import {Button, SelectProps} from "@arco-design/web-react";
import { Slot } from "@/graph-next/type";
import {IconPlus} from "@arco-design/web-react/icon";

/**
 * 继承 select props,增加了onChange、previewRes事件
 */
interface SlotListSelectorProps extends Omit<SelectProps, 'onChange'> {
    onChange?: (value: string | string[], options: Slot | Slot[]) => void;
    previewRes?: (res: Slot[]) => Slot & { disabled?: boolean };
}
const SlotListSelector: React.FC<SlotListSelectorProps> = ({

}) => {

    return <>

        <Button
            type="outline"
            status="success"
            long={true}
            icon={<IconPlus />}
        >
           添加
        </Button>
    </>
}

export default SlotListSelector;